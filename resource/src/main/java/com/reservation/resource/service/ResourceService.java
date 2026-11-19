package com.reservation.resource.service;

import com.reservation.resource.dto.ResourceRequest;
import com.reservation.resource.dto.ResourceResponse;
import com.reservation.resource.entity.Resource;
import com.reservation.resource.exception.ResourceNotFoundException;
import com.reservation.resource.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository repository;

    private void validateHours(ResourceRequest request) {
        if (!request.openTime().isBefore(request.closeTime())) {
            throw new IllegalArgumentException("Opening time must be before close time");
        }
    }

    private void applyRequest(Resource resource, ResourceRequest request) {
        resource.setName(request.name());
        resource.setType(request.type());
        resource.setHourlyPrice(request.hourlyPrice());
        resource.setOpenTime(request.openTime());
        resource.setCloseTime(request.closeTime());
    }

    public ResourceResponse create(ResourceRequest request) {
        validateHours(request);
        Resource resource = new Resource();
        applyRequest(resource, request);
        Resource saved = repository.save(resource);
        return ResourceResponse.from(saved);
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        validateHours(request);
        Resource resource = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
        applyRequest(resource, request);
        return ResourceResponse.from(resource);
    }

    public ResourceResponse findById(Long id) {
        Resource resource = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
        return ResourceResponse.from(resource);
    }

    public List<ResourceResponse> findAll() {
        return repository.findByActiveTrue()
                .stream()
                .map(ResourceResponse::from)
                .toList();

    }

    @Transactional
    public void deactivate(Long id) {
        Resource resource = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
        resource.setActive(false);
    }
}
