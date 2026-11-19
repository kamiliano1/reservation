package com.reservation.resource.dto;

import com.reservation.resource.entity.ResourceType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ResourceRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull ResourceType type,
        @NotNull @DecimalMin("0") @DecimalMax("100000") BigDecimal hourlyPrice,
        @NotNull LocalTime openTime,
        @NotNull LocalTime closeTime
) {
}
