package com.reservation.booking;

import com.reservation.booking.clients.ResourceLookup;
import com.reservation.booking.dto.BookingRequest;
import com.reservation.booking.dto.BookingResponse;
import com.reservation.booking.dto.ResourceDto;
import com.reservation.booking.entity.Booking;
import com.reservation.booking.entity.BookingStatus;
import com.reservation.booking.entity.ResourceType;
import com.reservation.booking.exception.BookingConflictException;
import com.reservation.booking.exception.BookingNotConfirmableException;
import com.reservation.booking.exception.BookingNotFoundException;
import com.reservation.booking.exception.BookingNotCancellableException;
import com.reservation.booking.repository.BookingRepository;
import com.reservation.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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

    @MockitoBean
    ResourceLookup resourceLookup;

    private static final Instant DAY = Instant.now()
            .plus(2, ChronoUnit.DAYS)
            .truncatedTo(ChronoUnit.DAYS);

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        when(resourceLookup
                .getResource(11L))
                .thenReturn(resource(11L, 8, 20));
        when(resourceLookup
                .getResource(12L))
                .thenReturn(resource(12L, 8, 20));

    }

    private ResourceDto resource(Long id, int open, int close) {
        return new ResourceDto(11L, "TennisCourt 1st",
                ResourceType.TENNIS_COURT,
                new BigDecimal("100.00"),
                LocalTime.of(open, 0),
                LocalTime.of(close, 0));
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

    @Test
    void whenCancel() {
        // given
        BookingResponse response = service.create(booking(11L, 10, 12));
        Long id = response.id();
        // when
        service.cancel(id);
        // then
        Booking stored = repository.findById(id).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void throwWhenNotExistingCancel() {
        // given + when + then
        assertThatThrownBy(() -> service.cancel(1000L))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining("1000");
    }

    @Test
    void throwWhenCancelCancelled() {
        // given
        BookingResponse response = service.create(booking(11L, 10, 12));
        Long id = response.id();
        service.cancel(id);
        // when + then
        assertThatThrownBy(() -> service.cancel(id))
                .isInstanceOf(BookingNotCancellableException.class)
                .hasMessageContaining("CANCELLED");
    }


    @Test
    void throwWhenExpiredCancel() {
        // given
        BookingResponse response = service.create(booking(11L, 10, 12));
        Long id = response.id();
        Booking stored = repository.findById(id).orElseThrow();
        stored.setStatus(BookingStatus.EXPIRED);
        repository.save(stored);
        // when + then
        assertThatThrownBy(() -> service.cancel(id))
                .isInstanceOf(BookingNotCancellableException.class)
                .hasMessageContaining("EXPIRED");
    }

    @Test
    void whenCreatedCancelledAndCreatedSameHours() {
        // given
        BookingResponse response = service.create(booking(11L, 10, 12));
        Long id = response.id();
        service.cancel(id);
        // when
        service.create(booking(11L, 10, 12));
        // then
        assertThat(repository.findAll())
                .hasSize(2)
                .extracting(Booking::getStatus)
                .containsExactlyInAnyOrder(BookingStatus.CANCELLED, BookingStatus.PENDING);

    }

    @Test
    void whenConfirm() {
        // given
        BookingResponse response = service.create(booking(11L, 10, 12));
        Long id = response.id();
        // when
        service.confirm(id);
        // then
        Booking stored = repository.findById(id).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void throwWhenConfirmNotExisted() {
        // given + when + then
        assertThatThrownBy(() -> service.confirm(100000L))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining("100000");
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
    void throwWhenConfirmNotPending(BookingStatus status) {
        // given
        BookingResponse response = service.create(booking(11L, 10, 12));
        Long id = response.id();
        Booking stored = repository.findById(id).orElseThrow();
        stored.setStatus(status);
        repository.save(stored);
        // when + then
        assertThatThrownBy(() -> service.confirm(id))
                .isInstanceOf(BookingNotConfirmableException.class)
                .hasMessageContaining(status.name());
    }


    @Test
    void throwWhenConfirmAndCreate() {
        // given
        BookingResponse response = service.create(booking(11L, 10, 12));
        Long id = response.id();
        service.confirm(id);
        // when + then
        assertThatThrownBy(() -> service.create(booking(11L, 10, 12)))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("Resource is already booked in this time range");
    }
}