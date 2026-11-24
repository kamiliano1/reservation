package com.reservation.booking;

import com.reservation.booking.dto.BookingRequest;
import com.reservation.booking.dto.BookingResponse;
import com.reservation.booking.entity.Booking;
import com.reservation.booking.entity.BookingStatus;
import com.reservation.booking.exception.BookingConflictException;
import com.reservation.booking.repository.BookingRepository;
import com.reservation.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.username=test",
        "spring.datasource.password=test",
        "spring.datasource.url=jdbc:postgresql://localhost:1/test",
})
@Import(TestcontainersConfiguration.class)
public class BookingServiceTest {

    @Autowired
    BookingRepository repository;
    @Autowired
    BookingService service;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    private BookingRequest booking(Long resourceId) {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        return new BookingRequest(resourceId, "testUser", start, end);
    }
    
    @Test
    void createBooking() {
        // given
        BookingRequest request = booking(11L);
        // when
        BookingResponse response = service.create(request);
        // then
        assertThat(response.id()).isNotNull();
        assertThat(response.userId()).isEqualTo("testUser");
        assertThat(response.status()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void throwWhenTwoReservationsWithSameResourceIdAndTimesOverlap() {
        // given
        BookingRequest request = booking(11L);
        service.create(request);
        // when + then
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("Resource is already booked in this time range");
    }

    //
//    @Test
//    void throwWhenTwoReservationsWithSameResourceIdPendingAndTimesOverlap() {
//        // given
//        repository.saveAndFlush(booking(11L, 12, 14, BookingStatus.PENDING));
//        // when + then
//        assertThatThrownBy(() -> repository.saveAndFlush(booking(11L, 13, 14, BookingStatus.PENDING)))
//                .isInstanceOf(DataIntegrityViolationException.class)
//                .hasMessageContaining("no_overlapping_bookings");
//    }
//
    @Test
    void whenTwoReservationsWithTheSameResourceIdConfirmedAndTimesNotOverlap() {
        // given
        service.create(booking(11L, 2L));
        // when
        service.create(booking(11L, 4L));
        // then
        List<BookingResponse> bookingList = service.findAll();
        assertThat(bookingList).hasSize(2);
    }
//
//    @Test
//    void whenTwoReservationsWithTheSameResourceIdConfirmedAndTimesNotOverlapSecondWay() {
//        // given
//        repository.saveAndFlush(booking(11L, 13, 14, BookingStatus.CONFIRMED));
//        // when
//        repository.saveAndFlush(booking(11L, 12, 13, BookingStatus.CONFIRMED));
//        // then
//        List<Booking> bookingList = repository.findAll();
//        assertThat(bookingList).hasSize(2);
//    }
//
//    @Test
//    void whenTwoReservationsWithTheDifferentResourceIdConfirmedAndTimesOverlap() {
//        // given
//        repository.saveAndFlush(booking(11L, 12, 13, BookingStatus.CONFIRMED));
//        //  when
//        repository.saveAndFlush(booking(12L, 12, 13, BookingStatus.CONFIRMED));
//        // then
//        List<Booking> bookingList = repository.findAll();
//        assertThat(bookingList).hasSize(2);
//    }
//
//    @Test
//    void whenTwoReservationsWithTheSameResourceIdConfirmedAndCancelledTimesOverlap() {
//        // given
//        repository.saveAndFlush(booking(11L, 12, 13, BookingStatus.CONFIRMED));
//        // when
//        repository.saveAndFlush(booking(11L, 12, 13, BookingStatus.CANCELLED));
//        // then
//        List<Booking> bookingList = repository.findAll();
//        assertThat(bookingList).hasSize(2);
//    }
//
//    @Test
//    void whenTwoReservationsWithTheSameResourceIdCancelledAndConfirmedTimesOverlap() {
//        // given
//        repository.saveAndFlush(booking(11L, 12, 13, BookingStatus.CANCELLED));
//        // when
//        repository.saveAndFlush(booking(11L, 12, 13, BookingStatus.CONFIRMED));
//        // then
//        List<Booking> bookingList = repository.findAll();
//        assertThat(bookingList).hasSize(2);
//    }
//
//    @Test
//    void throwWhenWrongHours() {
//        // given + when + then
//        assertThatThrownBy(() -> repository.saveAndFlush(booking(11L, 14, 13, BookingStatus.CONFIRMED)))
//                .isInstanceOf(DataIntegrityViolationException.class)
//                .hasMessageContaining("chk_booking_times");
//    }
//
//    @Test
//    void whenReservationCreatedAsConfirmedSwitchedToCancelledAndSecondConfirmedWithTheSameHours() {
//        // given
//        Booking booking = repository.saveAndFlush(booking(11L, 12, 13, BookingStatus.CONFIRMED));
//        booking.setStatus(BookingStatus.CANCELLED);
//        // when
//        repository.saveAndFlush(booking);
//        repository.saveAndFlush(booking(12L, 12, 13, BookingStatus.CONFIRMED));
//        // then
//        List<Booking> bookingList = repository.findAll();
//        assertThat(bookingList).hasSize(2);
//        assertThat(bookingList).extracting(Booking::getStatus)
//                .containsExactly(BookingStatus.CANCELLED, BookingStatus.CONFIRMED);
//    }
}