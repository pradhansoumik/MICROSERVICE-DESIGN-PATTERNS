package com.interview.cqrs.api;

import com.interview.cqrs.command.CancelOrderCommand;
import com.interview.cqrs.command.OrderCommandService;
import com.interview.cqrs.command.OrderWriteModel;
import com.interview.cqrs.command.PlaceOrderCommand;
import com.interview.cqrs.query.OrderQueryService;
import com.interview.cqrs.query.OrderReadModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Separate command vs query HTTP APIs — classic CQRS API shape.
 *
 * Commands: POST /api/commands/...
 * Queries:  GET  /api/queries/...
 */
@RestController
@RequestMapping("/api")
public class CqrsController {

    private final OrderCommandService commandService;
    private final OrderQueryService queryService;

    public CqrsController(OrderCommandService commandService, OrderQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    // ---------- COMMAND APIs (write) ----------

    @PostMapping("/commands/orders")
    public Map<String, Object> placeOrder(@RequestBody PlaceOrderCommand command) {
        OrderWriteModel saved = commandService.placeOrder(command);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("side", "COMMAND / WRITE");
        body.put("orderId", saved.getOrderId());
        body.put("status", saved.getStatus());
        body.put("message", "Saved to WRITE model; read model projected asynchronously (in-process event)");
        return body;
    }

    @PostMapping("/commands/orders/{orderId}/cancel")
    public Map<String, Object> cancel(@PathVariable String orderId) {
        try {
            OrderWriteModel saved = commandService.cancelOrder(new CancelOrderCommand(orderId));
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("side", "COMMAND / WRITE");
            body.put("orderId", saved.getOrderId());
            body.put("status", saved.getStatus());
            body.put("message", "WRITE model updated; READ model will refresh via projection");
            return body;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    // ---------- QUERY APIs (read) ----------

    @GetMapping("/queries/orders/{orderId}")
    public Map<String, Object> getOrder(@PathVariable String orderId) {
        try {
            OrderReadModel view = queryService.getById(orderId);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("side", "QUERY / READ");
            body.put("order", view);
            return body;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping("/queries/orders")
    public Map<String, Object> listOrders(@RequestParam(required = false) String customerId) {
        List<OrderReadModel> views = (customerId == null || customerId.isBlank())
                ? queryService.listAll()
                : queryService.listByCustomer(customerId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("side", "QUERY / READ");
        body.put("count", views.size());
        body.put("orders", views);
        return body;
    }
}
