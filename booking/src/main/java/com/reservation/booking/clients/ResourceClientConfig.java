package com.reservation.booking.clients;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;


@Configuration
@ImportHttpServices(group = "resource", types = {ResourceClient.class})
public class ResourceClientConfig {

}

