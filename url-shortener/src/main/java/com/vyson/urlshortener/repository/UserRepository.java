package com.vyson.urlshortener.repository;

import com.vyson.urlshortener.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByApiKey(String apiKey);

    Optional<User> findByEmail(String email);

    boolean existsByApiKey(String apiKey);

    boolean existsByEmail(String email);
}
