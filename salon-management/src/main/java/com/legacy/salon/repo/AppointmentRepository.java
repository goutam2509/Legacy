package com.legacy.salon.repo;

import com.legacy.salon.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findAllByOrderByStartTimeDesc();

    long countByStartTimeBetweenAndStatusNot(LocalDateTime from, LocalDateTime to, Appointment.Status status);

    List<Appointment> findByStartTimeBetweenAndStatusNotOrderByStartTimeAsc(
            LocalDateTime from, LocalDateTime to, Appointment.Status status);

    long countBySourceAndSeen(String source, Boolean seen);

    List<Appointment> findBySourceAndSeenOrderByStartTimeAsc(String source, Boolean seen);

    long countByCustomerId(Long customerId);

    long countByServiceId(Long serviceId);

    @Query("select coalesce(sum(a.price), 0) from Appointment a "
            + "where a.status = :status "
            + "and a.startTime >= :from and a.startTime < :to")
    BigDecimal revenueBetween(@Param("status") Appointment.Status status,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);
}
