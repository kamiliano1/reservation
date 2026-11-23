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
public class BookingServiceTest {

    @Autowired
    BookingRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    private Booking bookingConfirmed(Long resourceId, String startTime, String endTime) {
        Booking booking = new Booking();
        booking.setResourceId(resourceId);
        booking.setUserId("New User");
        booking.setStartTime(Instant.parse("2025-12-27T" + startTime + ":00:00Z"));
        booking.setEndTime(Instant.parse("2025-12-27T" + endTime + ":00:00Z"));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(Instant.now());
        booking.setUpdatedAt(Instant.now());
        return booking;
    }

    private Booking bookingCancelled(Long resourceId, String startTime, String endTime) {
        Booking booking = new Booking();
        booking.setResourceId(resourceId);
        booking.setUserId("New User");
        booking.setStartTime(Instant.parse("2025-12-27T" + startTime + ":00:00Z"));
        booking.setEndTime(Instant.parse("2025-12-27T" + endTime + ":00:00Z"));
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCreatedAt(Instant.now());
        booking.setUpdatedAt(Instant.now());
        return booking;
    }

    @Test
    void createBooking() {
        // given + when
        Booking booking = repository.saveAndFlush(bookingConfirmed(11L, "12", "13"));
        Booking imported = repository.findById(booking.getId()).orElseThrow();
        // then
        assertThat(booking.getId()).isEqualTo(imported.getId());
    }

    @Test
    void throwWhenTwoReservationsWithSameResourceIdConfirmedAndTimesOverlay() {
        // given
        repository.saveAndFlush(bookingConfirmed(11L, "12", "14"));
        // when + then
        assertThatThrownBy(() -> repository.saveAndFlush(bookingConfirmed(11L, "13", "14")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("no_overlapping_bookings");
    }

    @Test
    void whenTwoReservationsWithTheSameResourceIdConfirmedAndTimesNotOverlay() {
        // given + when
        repository.saveAndFlush(bookingConfirmed(11L, "12", "13"));
        repository.saveAndFlush(bookingConfirmed(11L, "13", "14"));
        List<Booking> bookingList = repository.findAll();
        // then
        assertThat(bookingList).hasSize(2);
    }

    @Test
    void whenTwoReservationsWithTheDifferentResourceIdConfirmedAndTimesOverlay() {
        // given + when
        repository.saveAndFlush(bookingConfirmed(11L, "12", "13"));
        repository.saveAndFlush(bookingConfirmed(12L, "12", "13"));
        List<Booking> bookingList = repository.findAll();
        // then
        assertThat(bookingList).hasSize(2);
    }

    @Test
    void whenTwoReservationsWithTheSameResourceIdConfirmedAndCancelledTimesOverlay() {
        // given + when
        repository.saveAndFlush(bookingConfirmed(11L, "12", "13"));
        repository.saveAndFlush(bookingCancelled(11L, "12", "13"));
        List<Booking> bookingList = repository.findAll();
        // then
        assertThat(bookingList).hasSize(2);
    }

    @Test
    void whenTwoReservationsWithTheSameResourceIdCancelledAndConfirmedTimesOverlay() {
        // given + when
        repository.saveAndFlush(bookingCancelled(11L, "12", "13"));
        repository.saveAndFlush(bookingConfirmed(11L, "12", "13"));
        List<Booking> bookingList = repository.findAll();
        // then
        assertThat(bookingList).hasSize(2);
    }

    @Test
    void throwWhenWrongHours() {
        // given + when + then
        assertThatThrownBy(() -> repository.saveAndFlush(bookingCancelled(11L, "14", "13")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_booking_times");
    }
}