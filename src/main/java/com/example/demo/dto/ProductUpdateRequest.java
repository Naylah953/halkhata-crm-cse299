package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductUpdateRequest {
    private BigDecimal price;
    private Integer quantity;
}