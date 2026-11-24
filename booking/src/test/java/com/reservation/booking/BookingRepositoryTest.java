package com.reservation.booking;

import com.reservation.booking.entity.Booking;
import com.reservation.booking.entity.BookingStatus;
import com.reservation.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.username=test",
        "spring.datasource.password=test",
        "spring.datasource.url=jdbc:postgresql://localhost:1/test",
})
@Import(TestcontainersConfiguration.class)
public class BookingRepositoryTest {

    @Autowired
    BookingRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    private Booking booking(Long resourceId, long startTime, long endTime, BookingStatus status) {
        Booking booking = new Booking();
        booking.setResourceId(resourceId);
        booking.setUserId("New User");
        Instant time = Instant.parse("2026-07-10T00:00:00Z");
        booking.setStartTime(time.plusSeconds(3600 * startTime));
        booking.setEndTime(time.plusSeconds(3600 * endTime));
        booking.setStatus(status);
        return booking;
    }

    @Test
    void createBooking() {
        // given
        Booking booking = repository.saveAndFlush(booking(11L, 12, 13, BookingStatus.CONFIRMED));
        // when
        Booking imported = repository.findById(booking.getId()).orElseThrow();
        // then
        assertThat(booking.getId()).isEqualTo(imported.getId());
    }

    @Test
    void throwWhenTwoReservationsWithSameResourceIdConfirmedAndTimesOverlap() {
        // given
        repository.saveAndFlush(booking(11L, 12, 14, BookingStatus.CONFIRMED));
        // when + then
        assertThatThrownBy(() -> repository.saveAndFlush(booking(11L, 13, 14, BookingStatus.CONFIRMED)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("no_overlapping_bookings");
    }

    @Test
    void throwWhenTwoReservationsWithSameResourceIdPendingAndTimesOverlap() {
        // given
        repository.saveAndFlush(booking(11L, 12, 14, BookingStatus.PENDING));
        // when + then
        assertThatThrownBy(() -> repository.saveAndFlush(booking(11L, 13, 14, BookingStatus.PENDING)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("no_overlapping_bookings");
    }

    @Test
    void whenTwoReservationsWithTheSameResourceIdConfirmedAndTimesNotOverlap() {
        // given
        repository.saveAndFlush(booking(11L, 12, 13, BookingStatus.CONFIRMED));
        // when
        repository.saveAndFlush(booking(11L, 13, 14, BookingStatus.CONFIRMED));
        // then
        List<Booking> bookingList = repository.findAll();
        assertThat(bookingList).hasSize(2);
    }

    @Test
    void whenTwoReservationsWithTheSameResourceIdConfirmedAndTimesNotOverlapSecondWay() {
        // given
        repository.saveAndFlush(booking(11L, 13, 14, BookingStatus.CONFIRMED));
        // when
        repository.saveAndFlush(booking(11L, 12, 13, BookingStatus.CONFIRMED));
        // then
        List<Booking> bookingList = repository.findAll();
        assertThat(bookingList).hasSize(2);
    }

    @Test
    void whenTwoReservationsWithTheDifferentResourceIdConfirmedAndTimesOverlap() {
        // given
        repository.saveAndFlush(booking(11L, 12, 13, BookingStatus.CONFIRMED));
        //  when
        repository.saveAndFlush(booking(12L, 12, 13, BookingStatus.CONFIRMED));
        // then
        List<Booking> bookingList = repository.findAll();
        assertThat(bookingList).hasSize(2);
    }

    @Test
    void whenTwoReservationsWithTheSameResourceIdConfirmedAndCancelledTimesOverlap() {
        // given
        repository.saveAndFlush(booking(11L, 12, 13, BookingStatus.CONFIRMED));
        // when
        repository.saveAndFlush(booking(11L, 12, 13, BookingStatus.CANCELLED));
        // then
        List<Booking> bookingList = repository.findAll();
        assertThat(bookingList).hasSize(2);
    }

    @Test
    void whenTwoReservationsWithTheSameResourceIdCancelledAndConfirmedTimesOverlap() {
        // given
        repository.saveAndFlush(booking(11L, 12, 13, BookingStatus.CANCELLED));
        // when
        repository.saveAndFlush(booking(11L, 12, 13, BookingStatus.CONFIRMED));
        // then
        List<Booking> bookingList = repository.findAll();
        assertThat(bookingList).hasSize(2);
    }

    @Test
    void throwWhenWrongHours() {
        // given + when + then
        assertThatThrownBy(() -> repository.saveAndFlush(booking(11L, 14, 13, BookingStatus.CONFIRMED)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_booking_times");
    }

    @Test
    void whenReservationCreatedAsConfirmedSwitchedToCancelledAndSecondConfirmedWithTheSameHours() {
        // given
        Booking booking = repository.saveAndFlush(booking(11L, 12, 13, BookingStatus.CONFIRMED));
        booking.setStatus(BookingStatus.CANCELLED);
        // when
        repository.saveAndFlush(booking);
        repository.saveAndFlush(booking(12L, 12, 13, BookingStatus.CONFIRMED));
        // then
        List<Booking> bookingList = repository.findAll();
        assertThat(bookingList).hasSize(2);
        assertThat(bookingList).extracting(Booking::getStatus)
                .containsExactly(BookingStatus.CANCELLED, BookingStatus.CONFIRMED);
    }
}