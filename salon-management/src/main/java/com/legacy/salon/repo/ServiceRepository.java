package com.legacy.salon.repo;

import com.legacy.salon.model.SalonService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<SalonService, Long> {
    List<SalonService> findAllByOrderByNameAsc();
}
