package com.reservation.booking.exception;

public class ResourceServiceUnavailableException extends RuntimeException {

    public ResourceServiceUnavailableException(Throwable cause, String message) {
        super(message, cause);
    }
}
