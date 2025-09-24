package com.example.logging.repository;

import com.example.logging.entity.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, UUID> {
    List<EventLog> findByTimestampBetween(Instant startDate, Instant endDate);
}
