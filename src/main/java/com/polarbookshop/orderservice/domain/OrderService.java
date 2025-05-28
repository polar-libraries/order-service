package com.polarbookshop.orderservice.domain;

import com.polarbookshop.orderservice.book.Book;
import com.polarbookshop.orderservice.book.BookClient;
import com.polarbookshop.orderservice.event.OrderAcceptedMessage;
import com.polarbookshop.orderservice.event.OrderDispatchedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final BookClient bookClient;
    private final StreamBridge streamBridge;

    OrderService(OrderRepository orderRepository, BookClient bookClient, StreamBridge streamBridge) {
        this.bookClient = bookClient;
        this.orderRepository = orderRepository;
        this.streamBridge = streamBridge;
    }

    @Transactional
    public Mono<Order> submitOrder(String isbn, int quantity) {

        return bookClient.getBookByIsbn(isbn)
                .map(book -> buildAcceptedOrder(book, quantity))
                .defaultIfEmpty(buildRejectedOrder(isbn, quantity))
                .flatMap(orderRepository::save)
                .doOnNext(this::publishOrderAcceptedEvent);
    }

    public static Order buildAcceptedOrder(Book book, int quantity) {
        return Order.of(book.isbn(), book.title() + " - " + book.author(),
                book.price(), quantity, OrderStatus.ACCEPTED);
    }

    public static Order buildRejectedOrder(String bookIsbn, int quantity) {
        return Order.of(bookIsbn, null, null, quantity, OrderStatus.REJECTED);
    }

    public Flux<Order> getAllOrders(String userId) {
        return orderRepository.findAllByCreatedBy(userId);
    }

    public Flux<Order> consumeOrderDispatchedEvent(Flux<OrderDispatchedMessage> flux) {
        return flux.flatMap(message ->
                        orderRepository.findById(message.orderId()))
                .map(this::buildDispatchedOrder)
                .flatMap(orderRepository::save);
    }

    private Order buildDispatchedOrder(Order existingOrder) {
        return new Order(existingOrder.id(), existingOrder.bookIsbn(), existingOrder.bookName(),
                existingOrder.bookPrice(),
                existingOrder.quantity(), OrderStatus.DISPATCHED,
                existingOrder.createdDate(), existingOrder.lastModifiedDate(),
                existingOrder.createdBy(), existingOrder.lastModifiedBy(),
                existingOrder.version());
    }

    private void publishOrderAcceptedEvent(Order order) {
        if (!order.status().equals(OrderStatus.ACCEPTED)) {
            return;
        }
        var orderAcceptedMessage = new OrderAcceptedMessage(order.id());
        log.info("Enviando o pedido aceito event com id {}", order.id());

        var result = streamBridge.send("acceptOrder-out-0", orderAcceptedMessage);
        log.info("Resultado do pedido enviado com id {}: {}",order.id(), result);
    }
}
