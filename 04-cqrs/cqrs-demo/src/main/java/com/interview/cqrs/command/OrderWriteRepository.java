package com.interview.cqrs.command;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Simulates WRITE database (orders table). */
@Repository
public class OrderWriteRepository {

    private final Map<String, OrderWriteModel> store = new ConcurrentHashMap<>();

    public void save(OrderWriteModel order) {
        store.put(order.getOrderId(), order);
    }

    public Optional<OrderWriteModel> findById(String orderId) {
        return Optional.ofNullable(store.get(orderId));
    }
}
