package com.keda.k8s.product.app.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.keda.k8s.product.app.entity.Product;
import com.keda.k8s.product.app.entity.ProductCategory;

public interface ProductRepository extends JpaRepository<Product, Long> {

     Page<Product> findByCategory(ProductCategory category, Pageable pageable);

     @Query("SELECT p FROM Product p WHERE " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchProducts(
        @Param("category") ProductCategory category,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("search") String search,
        Pageable pageable
    );

    @Query("SELECT p.category, COUNT(p), SUM(p.stockQuantity) FROM Product p GROUP BY p.category")
    List<Object[]> getCategoryStatistics();

}
