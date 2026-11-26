package com.reservation.booking.entity;

public enum BookingStatus {
    PENDING, CONFIRMED, CANCELLED, EXPIRED;

    public boolean isActive() {
        return switch (this) {
            case PENDING, CONFIRMED -> true;
            case CANCELLED, EXPIRED -> false;
        };
    }

    public boolean isPending() {
        return this == PENDING;
    }
}
