package com.reservation.booking.service;

import com.reservation.booking.dto.BookingRequest;
import com.reservation.booking.dto.BookingResponse;
import com.reservation.booking.entity.Booking;
import com.reservation.booking.entity.BookingStatus;
import com.reservation.booking.repository.BookingRepository;
import com.reservation.booking.exception.BookingConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository repository;

    private void validateHours(BookingRequest request) {
        if (!request.startTime().isBefore(request.endTime())) {
            throw new IllegalArgumentException("Opening time must be before close time");
        }
    }

    private void applyRequest(Booking booking, BookingRequest request) {
        booking.setResourceId(request.resourceId());
        booking.setUserId(request.userId());
        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());
        booking.setStatus(BookingStatus.PENDING);
    }

    public BookingResponse create(BookingRequest request) {
        validateHours(request);
        Booking booking = new Booking();
        applyRequest(booking, request);
        try {
            Booking saved = repository.saveAndFlush(booking);
            return BookingResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMostSpecificCause().getMessage().contains("no_overlapping_bookings")) {
                throw new BookingConflictException();
            }
            throw ex;
        }
    }
}
