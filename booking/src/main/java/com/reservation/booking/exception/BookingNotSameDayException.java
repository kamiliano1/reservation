package com.reservation.booking.exception;

public class BookingNotSameDayException extends RuntimeException {
    public BookingNotSameDayException() {
        super("Booked reservation must start and end at the same day");
    }
}
