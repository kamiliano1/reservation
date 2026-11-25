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

    private static final Instant DAY = Instant.now()
            .plus(2, ChronoUnit.DAYS)
            .truncatedTo(ChronoUnit.DAYS);

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    private BookingRequest booking(Long resourceId, long fromHour, long toHour) {
        Instant start = DAY.plus(fromHour, ChronoUnit.HOURS);
        Instant end = DAY.plus(toHour, ChronoUnit.HOURS);
        return new BookingRequest(resourceId, "testUser", start, end);
    }


    @Test
    void createBooking() {
        // given
        BookingRequest request = booking(11L, 10, 12);
        // when
        BookingResponse response = service.create(request);
        // then
        assertThat(response.id()).isNotNull();
        assertThat(response.userId()).isEqualTo("testUser");
        assertThat(response.status()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void throwWhenTimesOverlap() {
        // given
        BookingRequest request = booking(11L, 10, 12);
        service.create(request);
        // when + then
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("Resource is already booked in this time range");
    }

    @Test
    void throwWhenTimesPartlyOverlap() {
        // given
        service.create(booking(11L, 10, 12));
        // when + then
        assertThatThrownBy(() -> service.create(booking(11L, 11, 13)))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("Resource is already booked in this time range");
    }

    @Test
    void throwsWhenOneInsideAnother() {
        // given
        service.create(booking(11L, 10, 14));
        // when + then
        assertThatThrownBy(() -> service.create(booking(11L, 11, 13)))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("Resource is already booked in this time range");
    }

    @Test
    void whenTimesNotOverlapBackToBack() {
        // given
        service.create(booking(11L, 10, 12));
        // when
        service.create(booking(11L, 12, 14));
        // then
        List<BookingResponse> bookingList = service.findAll();
        assertThat(bookingList).hasSize(2);
    }

    @Test
    void whenTimesNotOverlapBackToBackDifferentOrder() {
        // given
        service.create(booking(11L, 12, 14));
        // when
        service.create(booking(11L, 10, 12));
        // then
        List<BookingResponse> bookingList = service.findAll();
        assertThat(bookingList).hasSize(2);
    }

    @Test
    void allowsSameTimeOnDifferentResources() {
        // given
        service.create(booking(11L, 12, 14));
        // when
        service.create(booking(12L, 12, 14));
        // then
        List<BookingResponse> bookingList = service.findAll();
        assertThat(bookingList).hasSize(2);
    }

    @Test
    void throwWhenWrongHours() {
        // given + when + then
        assertThatThrownBy(() -> service.create(booking(11L, 12, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start time must be before end time");
    }

    @Test
    void throwWhenExactTime() {
        // given + when + then
        assertThatThrownBy(() -> service.create(booking(11L, 12, 12)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start time must be before end time");
    }

}