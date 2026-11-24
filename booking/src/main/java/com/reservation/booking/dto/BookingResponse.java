package com.reservation.booking.dto;

import com.reservation.booking.entity.Booking;
import com.reservation.booking.entity.BookingStatus;


import java.time.Instant;

public record BookingResponse(
        Long id,
        Long resourceId,
        String userId,
        Instant startTime,
        Instant endTime,
        BookingStatus status
) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getResourceId(),
                booking.getUserId(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus()
        );
    }
}
