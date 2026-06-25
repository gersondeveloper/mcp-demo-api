package org.gersondeveloper.domain.dto.response;

import org.gersondeveloper.domain.enums.OrderStatus;
import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        BigDecimal totalAmount,
        Long userId,
        List<OrderItemResponse> items) {}
