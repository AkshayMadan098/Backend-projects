package com.vyson.urlshortener.repository;

import com.vyson.urlshortener.entity.UrlMapping;
import com.vyson.urlshortener.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByShortCode(String shortCode);

    Optional<UrlMapping> findByShortCodeAndIsDeletedFalse(String shortCode);

    boolean existsByShortCode(String shortCode);

    boolean existsByShortCodeAndIsDeletedFalse(String shortCode);

    List<UrlMapping> findByUserAndIsDeletedFalseOrderByCreatedAtDesc(User user);

    List<UrlMapping> findByUserOrderByCreatedAtDesc(User user);

    // [Q1] Find last 10 URLs shortened
    List<UrlMapping> findTop10ByOrderByCreatedAtDesc();

    // [Q3] Find 10 most popular short codes by click count, breaking ties by last accessed time
    List<UrlMapping> findTop10ByIsDeletedFalseOrderByClickCountDescLastAccessedAtDesc();

    // [Q4] Group by originalUrl to find top 10 most shortened URLs
    @Query("SELECT u.originalUrl, COUNT(u) as timesShortened FROM UrlMapping u GROUP BY u.originalUrl ORDER BY timesShortened DESC")
    List<Object[]> findTopUrlsByFrequency();
}
