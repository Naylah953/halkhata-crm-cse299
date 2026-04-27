package com.dbinbox.aiinbox.service;

import com.dbinbox.aiinbox.model.Product;
import com.dbinbox.aiinbox.repository.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {
    @Autowired
    private ProductRepo productRepo;

    public String searchProducts(String query) {
        // This line fixes the "isEmpty" in Object error by explicitly using List<Product>
        List<Product> products = (List<Product>) productRepo.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query);

        if (products.isEmpty()) {
            return "Sorry, I couldn't find any products matching that.";
        }

        return products.stream()
                .map(p -> String.format("- %s: ৳%.2f (%d in stock)",
                        p.getName(), p.getPrice(), p.getQuantity()))
                .collect(Collectors.joining("\n"));
    }
}