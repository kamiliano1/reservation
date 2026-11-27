package com.reservation.booking.exception;

import java.time.LocalTime;

public class BookingOutsideOpeningHoursException extends RuntimeException {

    public BookingOutsideOpeningHoursException(LocalTime openTime, LocalTime closeTime) {
        super("Resource is open from: " + openTime.toString() + " to " + closeTime.toString());
    }
}
