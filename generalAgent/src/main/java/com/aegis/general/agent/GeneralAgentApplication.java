package com.aegis.general.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GeneralAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeneralAgentApplication.class, args);
    }

}
