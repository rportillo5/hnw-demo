package com.edwardjones.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HnwDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(HnwDemoApplication.class, args);
    }
}
