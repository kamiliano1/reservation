package com.reservation.booking.exception;

import com.reservation.booking.entity.BookingStatus;

public class BookingNotCancellableException extends RuntimeException {
    public BookingNotCancellableException(BookingStatus status) {
        super("Booking cannot be cancelled in status: " + status);
    }
}