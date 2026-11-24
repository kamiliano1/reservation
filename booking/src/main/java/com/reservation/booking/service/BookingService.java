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

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository repository;

    private void validateTime(BookingRequest request) {
        if (!request.startTime().isBefore(request.endTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
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
        validateTime(request);
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

    public List<BookingResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(BookingResponse::from)
                .toList();
    }
}
