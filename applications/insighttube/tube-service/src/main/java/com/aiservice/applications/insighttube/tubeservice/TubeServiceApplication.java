package com.aiservice.applications.insighttube.tubeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TubeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TubeServiceApplication.class, args);
    }
}
