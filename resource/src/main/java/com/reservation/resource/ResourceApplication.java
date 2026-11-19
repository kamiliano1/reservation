package com.reservation.resource;


import com.reservation.resource.dto.ResourceRequest;
import com.reservation.resource.entity.Resource;
import com.reservation.resource.entity.ResourceType;
import com.reservation.resource.repository.ResourceRepository;
import com.reservation.resource.service.ResourceService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.time.LocalTime;

@SpringBootApplication
public class ResourceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResourceApplication.class, args);
    }

//    @Bean
//    CommandLineRunner test(ResourceService service, ResourceRepository repository) {
//        return args -> {
//            ResourceRequest resource = new ResourceRequest("name",
//                    ResourceType.DESK,
//                    BigDecimal.valueOf(100.00),
//                    LocalTime.parse("14:00"),
//                    LocalTime.parse("12:00")
//            );
////            service.create(resource);
//            service.findById(11L);
//            // TODO 1: stwórz obiekt Resource i ustaw mu pola
//            // TODO 2: zapisz go przez repository
//            // TODO 3: wypisz zapisany obiekt
//        };
//    }
}
