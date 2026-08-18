package com.interview.cqrs.query;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Simulates READ database / projection store. */
@Repository
public class OrderReadRepository {

    private final Map<String, OrderReadModel> store = new ConcurrentHashMap<>();

    public void save(OrderReadModel view) {
        store.put(view.orderId(), view);
    }

    public Optional<OrderReadModel> findById(String orderId) {
        return Optional.ofNullable(store.get(orderId));
    }

    public List<OrderReadModel> findByCustomerId(String customerId) {
        return store.values().stream()
                .filter(v -> v.customerId().equals(customerId))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<OrderReadModel> findAll() {
        return new ArrayList<>(store.values());
    }
}
