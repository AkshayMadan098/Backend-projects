package com.vyson.urlshortener.repository;

import com.vyson.urlshortener.entity.RequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [Q1] Repository for persisting and retrieving request logs from database.
 */
@Repository
public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {

    List<RequestLog> findByHttpMethod(String httpMethod);

    List<RequestLog> findByClientIp(String clientIp);

    List<RequestLog> findByTimestampAfterOrderByTimestampDesc(LocalDateTime after);

    List<RequestLog> findAllByOrderByTimestampDesc();
}
