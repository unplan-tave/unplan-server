package com.unplan.unplanserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class UnplanServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnplanServerApplication.class, args);
    }

}
