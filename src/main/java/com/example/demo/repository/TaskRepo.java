package com.example.demo.repository;

import com.example.demo.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepo extends JpaRepository<Task, Long> {
    List<Task> findByTenantIdAndCompletedFalse(Long tenantId);
}