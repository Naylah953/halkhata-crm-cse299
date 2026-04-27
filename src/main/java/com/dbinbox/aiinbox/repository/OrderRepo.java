package com.dbinbox.aiinbox.repository;

import com.dbinbox.aiinbox.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepo extends JpaRepository<Order, UUID> {}