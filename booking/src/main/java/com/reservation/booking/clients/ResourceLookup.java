package com.reservation.booking.clients;

import com.reservation.booking.dto.ResourceDto;
import com.reservation.booking.exception.ResourceNotFoundException;
import com.reservation.booking.exception.ResourceServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResourceLookup {

    private final ResourceClient client;

    public ResourceDto getResource(Long id) {
        try {
            return client.getResource(id);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException(id);
        } catch (ResourceAccessException ex) {
            log.error(ex.getMessage());
            throw new ResourceServiceUnavailableException(ex.getCause(), ex.getMessage());
        } catch (RestClientException ex) {
            throw new ResourceServiceUnavailableException(ex.getCause(), ex.getMessage());
        }
    }
}
