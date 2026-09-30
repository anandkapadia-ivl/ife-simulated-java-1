package com.ife.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IfeVulnerableApplication {
    public static void main(String[] args) {
        SpringApplication.run(IfeVulnerableApplication.class, args);
    }
}
