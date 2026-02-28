package com.badminton;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class BadmintonCourtManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(BadmintonCourtManagementApplication.class, args);
    }

}
