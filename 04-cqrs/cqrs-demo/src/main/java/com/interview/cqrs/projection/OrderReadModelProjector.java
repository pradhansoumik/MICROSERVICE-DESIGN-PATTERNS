package com.interview.cqrs.projection;

import com.interview.cqrs.query.OrderReadModel;
import com.interview.cqrs.query.OrderReadRepository;
import com.interview.cqrs.shared.OrderChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Keeps READ model in sync when WRITE side changes.
 * Production: Kafka consumer / CDC / async projector.
 * Demo: Spring in-process event (still shows the CQRS split).
 */
@Component
public class OrderReadModelProjector {

    private static final Logger log = LoggerFactory.getLogger(OrderReadModelProjector.class);

    private final OrderReadRepository readRepository;

    public OrderReadModelProjector(OrderReadRepository readRepository) {
        this.readRepository = readRepository;
    }

    @EventListener
    public void on(OrderChangedEvent event) {
        String summary = event.customerId() + " | " + event.productId()
                + " x" + event.quantity() + " | ₹" + event.amount()
                + " | " + event.status();

        OrderReadModel view = new OrderReadModel(
                event.orderId(),
                event.customerId(),
                event.productId(),
                event.quantity(),
                event.amount(),
                event.status(),
                summary
        );
        readRepository.save(view);
        log.info("READ MODEL projected/updated for {} → {}", event.orderId(), event.status());
    }
}
