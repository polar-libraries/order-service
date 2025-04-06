package com.polarbookshop.orderservice.web;

import com.polarbookshop.orderservice.domain.Order;
import com.polarbookshop.orderservice.domain.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequestMapping("/orders")
@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }


    @GetMapping
    public Flux<Order> getBookOrder() {
        return orderService.getAllOrders();
    }

    @PostMapping
    public Mono<Order> submitOrder(@Valid @RequestBody OrderRequest request) {
        return orderService.submitOrder(request.isbn(), request.quantity());
    }


}
