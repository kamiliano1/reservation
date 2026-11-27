package com.reservation.booking.clients;

import com.reservation.booking.dto.ResourceDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface ResourceClient {

    @GetExchange("/api/resources/{id}")
    ResourceDto getResource(@PathVariable Long id);
}