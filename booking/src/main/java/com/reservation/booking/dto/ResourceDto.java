package com.reservation.booking.dto;

import com.reservation.booking.entity.ResourceType;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ResourceDto(
        Long id,
        String name,
        ResourceType type,
        BigDecimal hourlyPrice,
        LocalTime openTime,
        LocalTime closeTime
) {
}

