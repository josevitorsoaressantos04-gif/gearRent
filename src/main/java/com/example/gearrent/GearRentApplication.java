package com.example.gearrent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
public class GearRentApplication {

    public static void main(String[] args) {
        SpringApplication.run(GearRentApplication.class, args);
    }

}
