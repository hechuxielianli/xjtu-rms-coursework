package com.example.rms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/** G3-A skeleton: no business endpoints, accounts or generated credentials. */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class RmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(RmsApplication.class, args);
    }
}
