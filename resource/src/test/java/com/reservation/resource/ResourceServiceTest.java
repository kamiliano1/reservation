package com.reservation.resource;

import com.reservation.resource.dto.ResourceRequest;
import com.reservation.resource.dto.ResourceResponse;
import com.reservation.resource.entity.Resource;
import com.reservation.resource.entity.ResourceType;
import com.reservation.resource.exception.ResourceNotFoundException;
import com.reservation.resource.repository.ResourceRepository;
import com.reservation.resource.service.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.username=test",
        "spring.datasource.password=test",
        "spring.datasource.url=jdbc:postgresql://localhost:1/test",
})
@Import(TestcontainersConfiguration.class)
public class ResourceServiceTest {
    @Autowired
    ResourceService service;

    @Autowired
    ResourceRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    private ResourceRequest request() {
        return new ResourceRequest("Name", ResourceType.DESK,
                new BigDecimal("60.00"), LocalTime.of(9, 0), LocalTime.of(13, 0));
    }

    private ResourceRequest request(LocalTime open, LocalTime close) {
        return new ResourceRequest("Name", ResourceType.DESK,
                new BigDecimal("60.00"), open, close);
    }

    private ResourceRequest request(String name, BigDecimal hourlyPrice, LocalTime open, LocalTime close) {
        return new ResourceRequest(name, ResourceType.DESK,
                hourlyPrice, open, close);
    }

    @Test
    void createsResource() {
        // given
        ResourceRequest request = request();
        // when
        ResourceResponse result = service.create(request);
        // then
        assertThat(result.id()).isNotNull();
        assertThat(result.name()).isEqualTo("Name");
    }

    @Test
    void findCreatedResource() {
        // given
        ResourceRequest request = request();
        // when
        ResourceResponse created = service.create(request);
        ResourceResponse result = service.findById(created.id());
        // then
        assertThat(result.id()).isEqualTo(created.id());
        assertThat(created.name()).isEqualTo(result.name());
    }

    @Test
    void throwWhenResourceNotFound() {
        // when + then
        assertThatThrownBy(() -> service.findById(1111L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("1111");
    }

    @Test
    void throwWhenWrongHours() {
        // given
        ResourceRequest request = request(LocalTime.of(19, 0), LocalTime.of(13, 0));// when + then
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Opening time must be before close time");
    }

    @Test
    void throwWhenSameHours() {
        // given
        ResourceRequest request = request(LocalTime.of(13, 0), LocalTime.of(13, 0)); // when + then
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Opening time must be before close time");
    }

    @Test
    void throwWhenResourceDeleted() {
        // given
        ResourceRequest request = request();
        ResourceResponse result = service.create(request);
        Long id = result.id();
        // when
        service.deactivate(id);
        // then
        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(id));
    }

    @Test
    void deactivateMarksResourceInactiveAndHidesIt() {
        // given
        ResourceRequest request = request();
        ResourceResponse result = service.create(request);
        Long id = result.id();
        // when
        service.deactivate(id);
        // then
        Resource deactivated = repository.findById(id).orElseThrow();
        assertThat(deactivated.isActive()).isFalse();
    }

    @Test
    void whenTwoResourcesCreatedAndOneDeleted() {
        // given
        ResourceRequest request = request();
        ResourceResponse result = service.create(request);
        service.create(request("NameUpdated",
                new BigDecimal("160.00"), LocalTime.of(13, 0), LocalTime.of(15, 0))
        );
        Long id = result.id();
        // when
        service.deactivate(id);
        List<ResourceResponse> resources = service.findAll();
        // then
        assertThat(resources).hasSize(1);
        assertThat(resources).extracting(ResourceResponse::id).isNotEqualTo(id);

    }

    @Test
    void whenResourceUpdated() {
        // given
        ResourceRequest request = request();
        ResourceRequest updated = request("NameUpdated",
                new BigDecimal("160.00"), LocalTime.of(13, 0), LocalTime.of(15, 0));
        // when
        ResourceResponse result = service.create(request);
        Long id = result.id();
        service.update(id, updated);
        Resource resource = repository.findById(id).orElseThrow();
        // then
        assertThat(resource.getId()).isEqualTo(id);
        assertThat(resource.getName()).isEqualTo("NameUpdated");
        assertThat(resource.getOpenTime()).isEqualTo(LocalTime.of(13, 0));
        assertThat(resource.getHourlyPrice()).isEqualByComparingTo("160.00");
        assertThat(resource.getCloseTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(resource.getUpdatedAt()).isAfter(resource.getCreatedAt());
    }

    @Test
    void throwWhenNotExistedResourceUpdated() {
        // given
        ResourceRequest request = request();
        // when + then
        assertThatThrownBy(() -> service.update(11111L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("11111");
    }

    @Test
    void throwWhenDeletedResourceUpdated() {
        // given
        ResourceRequest request = request();
        ResourceResponse created = service.create(request);
        Long id = created.id();
        service.deactivate(id);

        // when + then
        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void throwWhenResourceUpdatedWithWrongHours() {
        // given
        ResourceRequest request = request();
        ResourceResponse created = service.create(request);
        ResourceRequest requestWrongHours = request(LocalTime.of(17, 0), LocalTime.of(15, 0));
        // when + then
        assertThatThrownBy(() -> service.update(created.id(), requestWrongHours))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Opening time must be before close time");
    }

    @Test
    void throwWhenResourceSaveUsingRepositoryWithWrongHours() {
        // given
        Resource resource = new Resource();
        resource.setName("Name");
        resource.setType(ResourceType.DESK);
        resource.setHourlyPrice(BigDecimal.valueOf(100));
        resource.setOpenTime(LocalTime.of(11, 0));
        resource.setCloseTime(LocalTime.of(9, 0));
        // when + then
        assertThatThrownBy(() -> repository.saveAndFlush(resource))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_hours");
    }
}
