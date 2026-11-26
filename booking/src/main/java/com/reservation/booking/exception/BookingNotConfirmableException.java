package com.reservation.booking.exception;

import com.reservation.booking.entity.BookingStatus;

public class BookingNotConfirmableException extends RuntimeException {

    public BookingNotConfirmableException(BookingStatus status) {
        super("Booking cannot be confirmed in status: " + status);
    }
}
