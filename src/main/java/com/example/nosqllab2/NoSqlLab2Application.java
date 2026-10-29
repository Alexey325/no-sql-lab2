package com.example.nosqllab2;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.SpringApplication;

// пользователи приходят только из JWT, поэтому дефолтный in-memory пользователь Spring Security не нужен
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class NoSqlLab2Application {

    public static void main(String[] args) {
        SpringApplication.run(NoSqlLab2Application.class, args);
    }

}
