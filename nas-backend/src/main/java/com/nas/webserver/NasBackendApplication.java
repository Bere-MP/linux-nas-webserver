package com.nas.webserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NasBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(NasBackendApplication.class, args);
    }
}
