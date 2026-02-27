package com.example.antivirus.security;

import com.example.antivirus.user.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeeder {

    @Bean
    CommandLineRunner seedAdmin(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            users.findByUsername("admin").orElseGet(() ->
                    users.save(User.builder()
                            .username("admin")
                            .passwordHash(encoder.encode("admin12345"))
                            .role(Role.ROLE_ADMIN)
                            .enabled(true)
                            .build())
            );
        };
    }
}