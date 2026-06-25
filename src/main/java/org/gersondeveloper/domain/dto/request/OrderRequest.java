package org.gersondeveloper.domain.dto.request;

import java.util.List;

public record OrderRequest(Long userId, List<OrderItemRequest> items) {}
