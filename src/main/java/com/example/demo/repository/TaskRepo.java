package com.example.demo.repository;

import com.example.demo.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepo extends JpaRepository<Task, Long> {

    // Used by AI to find pending tasks for the admin's shop
    List<Task> findByTenantIdAndCompletedFalse(Long tenantId);
}