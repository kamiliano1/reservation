package com.reservation.resource.dto;

import com.reservation.resource.entity.Resource;
import com.reservation.resource.entity.ResourceType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ResourceResponse(
        Long id,
        String name,
        ResourceType type,
        BigDecimal hourlyPrice,
        LocalTime openTime,
        LocalTime closeTime
) {


    public static ResourceResponse from(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getType(),
                resource.getHourlyPrice(),
                resource.getOpenTime(),
                resource.getCloseTime());
    }
}
