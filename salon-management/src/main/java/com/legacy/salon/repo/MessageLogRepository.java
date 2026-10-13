package com.legacy.salon.repo;

import com.legacy.salon.model.MessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {
    List<MessageLog> findTop100ByOrderByCreatedAtDesc();
}
