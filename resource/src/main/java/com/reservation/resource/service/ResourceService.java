package com.reservation.resource.service;

import com.reservation.resource.dto.ResourceRequest;
import com.reservation.resource.dto.ResourceResponse;
import com.reservation.resource.entity.Resource;
import com.reservation.resource.exception.ResourceNotFoundException;
import com.reservation.resource.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository repository;

    public ResourceResponse create(ResourceRequest request) {
        if (!request.openTime().isBefore(request.closeTime())) {
            throw new IllegalArgumentException("Opening time must be before close time");
        }
        Resource resource = new Resource();
        resource.setName(request.name());
        resource.setType(request.type());
        resource.setHourlyPrice(request.hourlyPrice());
        resource.setOpenTime(request.openTime());
        resource.setCloseTime(request.closeTime());

        Resource saved = repository.save(resource);
        return ResourceResponse.from(saved);
    }

    public ResourceResponse findById(Long id) {
        Resource resource = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
        return ResourceResponse.from(resource);
    }

    public List<ResourceResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(ResourceResponse::from)
                .toList();

    }
}
