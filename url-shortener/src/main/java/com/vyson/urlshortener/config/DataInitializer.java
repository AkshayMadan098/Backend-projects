package com.vyson.urlshortener.config;

import com.vyson.urlshortener.entity.User;
import com.vyson.urlshortener.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    public static final String ALICE_API_KEY = "ak_live_alice1234567890abcdef";
    public static final String BOB_API_KEY = "ak_live_bob1234567890abcdef";
    public static final String ENTERPRISE_API_KEY = "ak_live_enterprise9876543210fedcba";

    @Bean
    public CommandLineRunner initUsers(UserRepository userRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                User alice = new User("alice@example.com", "Alice Smith", ALICE_API_KEY, "hobby");
                User bob = new User("bob@example.com", "Bob Jones", BOB_API_KEY, "hobby");
                User enterprise = new User("enterprise@example.com", "Acme Corp Enterprise", ENTERPRISE_API_KEY, "enterprise");

                userRepository.saveAll(List.of(alice, bob, enterprise));
            }
        };
    }
}
