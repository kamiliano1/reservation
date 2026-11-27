package com.reservation.booking.service;

import com.reservation.booking.clients.ResourceLookup;
import com.reservation.booking.dto.BookingRequest;
import com.reservation.booking.dto.BookingResponse;
import com.reservation.booking.dto.ResourceDto;
import com.reservation.booking.entity.Booking;
import com.reservation.booking.entity.BookingStatus;
import com.reservation.booking.exception.*;
import com.reservation.booking.repository.BookingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository repository;
    private final ResourceLookup resourceLookup;

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

    private void validateOpeningHours(BookingRequest request, ResourceDto resource) {

        ZonedDateTime start = request.startTime().atZone(ZoneOffset.UTC);
        LocalDate startDate = start.toLocalDate();
        LocalTime startTime = start.toLocalTime();
        ZonedDateTime end = request.endTime().atZone(ZoneOffset.UTC);
        LocalDate endDate = end.toLocalDate();
        LocalTime endTime = end.toLocalTime();
        if (!startDate.isEqual(endDate)) {
            throw new BookingNotSameDayException();
        }
        if (startTime.isBefore(resource.openTime()) || endTime.isAfter(resource.closeTime())) {
            throw new BookingOutsideOpeningHoursException(resource.openTime(), resource.closeTime());
        }
    }

    public BookingResponse create(BookingRequest request) {
        validateTime(request);
        ResourceDto resource = resourceLookup.getResource(request.resourceId());
        validateOpeningHours(request, resource);
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

    @Transactional
    public void cancel(Long id) {
        Booking booking = repository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));
        BookingStatus status = booking.getStatus();
        if (!status.isActive()) {
            throw new BookingNotCancellableException(status);
        }
        booking.setStatus(BookingStatus.CANCELLED);
    }

    @Transactional
    public BookingResponse confirm(Long id) {
        Booking booking = repository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));
        BookingStatus status = booking.getStatus();
        if (!status.isPending()) {
            throw new BookingNotConfirmableException(status);
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        return BookingResponse.from(booking);
    }

    public List<BookingResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(BookingResponse::from)
                .toList();
    }
}
