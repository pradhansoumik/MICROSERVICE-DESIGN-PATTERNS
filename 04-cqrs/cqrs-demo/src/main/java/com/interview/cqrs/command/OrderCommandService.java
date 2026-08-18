package com.interview.cqrs.command;

import com.interview.cqrs.shared.OrderChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * COMMAND side — handles writes only. Does not serve dashboard queries.
 */
@Service
public class OrderCommandService {

    private static final Logger log = LoggerFactory.getLogger(OrderCommandService.class);

    private final OrderWriteRepository writeRepository;
    private final ApplicationEventPublisher events;

    public OrderCommandService(OrderWriteRepository writeRepository,
                               ApplicationEventPublisher events) {
        this.writeRepository = writeRepository;
        this.events = events;
    }

    public OrderWriteModel placeOrder(PlaceOrderCommand command) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        OrderWriteModel order = new OrderWriteModel(
                orderId,
                command.customerId(),
                command.productId(),
                command.quantity(),
                command.amount(),
                "CREATED"
        );
        writeRepository.save(order);
        log.info("WRITE MODEL saved {}", orderId);

        events.publishEvent(new OrderChangedEvent(
                order.getOrderId(), order.getCustomerId(), order.getProductId(),
                order.getQuantity(), order.getAmount(), order.getStatus()
        ));
        return order;
    }

    public OrderWriteModel cancelOrder(CancelOrderCommand command) {
        OrderWriteModel order = writeRepository.findById(command.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + command.orderId()));

        order.setStatus("CANCELLED");
        writeRepository.save(order);
        log.info("WRITE MODEL cancelled {}", order.getOrderId());

        events.publishEvent(new OrderChangedEvent(
                order.getOrderId(), order.getCustomerId(), order.getProductId(),
                order.getQuantity(), order.getAmount(), order.getStatus()
        ));
        return order;
    }
}
