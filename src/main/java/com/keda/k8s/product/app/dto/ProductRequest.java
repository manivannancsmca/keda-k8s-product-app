package com.keda.k8s.product.app.dto;

import java.math.BigDecimal;

import com.keda.k8s.product.app.entity.ProductCategory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductRequest {

    @NotBlank 
    @Size(max = 200)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull 
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @NotNull 
    @Min(0)
    private Integer stockQuantity;

    @NotNull
    private ProductCategory category;

    @Size(max = 500)
    private String imageUrl;
}
