package com.reservation.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record BookingRequest(
        @NotNull Long resourceId,
        @NotBlank @Size(max = 36) String userId,
        // TODO: Change userId to come from Keycloack not from request
        @NotNull @Future Instant startTime,
        @NotNull Instant endTime
) {

}
