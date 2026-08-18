package com.interview.cqrs.query;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * QUERY side — reads only from read model. Never writes to write DB.
 */
@Service
public class OrderQueryService {

    private final OrderReadRepository readRepository;

    public OrderQueryService(OrderReadRepository readRepository) {
        this.readRepository = readRepository;
    }

    public OrderReadModel getById(String orderId) {
        return readRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Read model not found: " + orderId));
    }

    public List<OrderReadModel> listByCustomer(String customerId) {
        return readRepository.findByCustomerId(customerId);
    }

    public List<OrderReadModel> listAll() {
        return readRepository.findAll();
    }
}
