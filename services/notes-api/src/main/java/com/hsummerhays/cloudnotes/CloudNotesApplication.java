package com.hsummerhays.cloudnotes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CloudNotesApplication {
    public static void main(String[] presumption) {
        SpringApplication.run(CloudNotesApplication.class, presumption);
    }
}
