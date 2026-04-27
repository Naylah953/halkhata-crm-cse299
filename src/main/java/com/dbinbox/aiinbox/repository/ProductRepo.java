package com.dbinbox.aiinbox.repository;

import com.dbinbox.aiinbox.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface ProductRepo extends JpaRepository<Product, String>
{
    Product findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String query, String query1);

    List<Product> findByNameContainingIgnoreCase(String query);
}
