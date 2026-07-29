package com.keda.k8s.product.app.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keda.k8s.product.app.dto.ProductRequest;
import com.keda.k8s.product.app.dto.ProductResponse;
import com.keda.k8s.product.app.entity.Product;
import com.keda.k8s.product.app.entity.ProductCategory;
import com.keda.k8s.product.app.exception.ProductNotFoundException;
import com.keda.k8s.product.app.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
            .name(request.getName())
            .description(request.getDescription())
            .price(request.getPrice())
            .stockQuantity(request.getStockQuantity())
            .category(request.getCategory())
            .imageUrl(request.getImageUrl())
            .build();
        
        Product saved = productRepository.save(product);
        log.info("Created product with id: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return productRepository.findById(id)
            .map(this::mapToResponse)
            .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(ProductCategory category, Pageable pageable) {
        return productRepository.findByCategory(category, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(
            ProductCategory category, 
            BigDecimal minPrice, 
            BigDecimal maxPrice, 
            String search, 
            Pageable pageable) {
        return productRepository.searchProducts(category, minPrice, maxPrice, search, pageable)
            .map(this::mapToResponse);
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));
        
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        
        Product updated = productRepository.save(product);
        log.info("Updated product with id: {}", updated.getId());
        return mapToResponse(updated);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException("Product not found: " + id);
        }
        productRepository.deleteById(id);
        log.info("Deleted product with id: {}", id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> getAllCategories() {
        return Arrays.stream(ProductCategory.values())
            .map(cat -> Map.of(
                "name", cat.name(),
                "displayName", cat.toString().replace("_", " ")
            ))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCategoryStatistics() {
        return productRepository.getCategoryStatistics().stream()
            .map(row -> Map.of(
                "category", row[0].toString(),
                "productCount", row[1],
                "totalStock", row[2]
            ))
            .collect(Collectors.toList());
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
            .id(product.getId())
            .name(product.getName())
            .description(product.getDescription())
            .price(product.getPrice())
            .stockQuantity(product.getStockQuantity())
            .category(product.getCategory())
            .imageUrl(product.getImageUrl())
            .createdAt(product.getCreatedAt())
            .updatedAt(product.getUpdatedAt())
            .build();
    }
}
