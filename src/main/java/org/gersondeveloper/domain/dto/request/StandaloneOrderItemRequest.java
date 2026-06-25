package org.gersondeveloper.domain.dto.request;

public record StandaloneOrderItemRequest(Long orderId, Long productId, int quantity) {}
