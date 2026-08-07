package com.keda.k8s.product.app.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.keda.k8s.product.app.entity.ProductCategory;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private ProductCategory category;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
