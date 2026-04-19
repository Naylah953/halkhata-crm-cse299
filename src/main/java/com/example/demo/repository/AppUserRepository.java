package com.example.demo.repository;

import com.example.demo.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByPhoneNumber(String phoneNumber);

    List<AppUser> findByTenantId(Long tenantId);
}
