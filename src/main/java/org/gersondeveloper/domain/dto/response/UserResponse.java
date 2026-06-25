package org.gersondeveloper.domain.dto.response;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String address,
        boolean isActive,
        LocalDateTime createDate) {}
