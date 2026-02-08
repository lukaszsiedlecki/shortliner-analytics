package com.shortliner.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShortlinerAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortlinerAnalyticsApplication.class, args);
    }
}
