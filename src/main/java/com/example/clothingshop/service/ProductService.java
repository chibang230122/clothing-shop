
package com.example.clothingshop.service;

import com.example.clothingshop.entity.Product;
import com.example.clothingshop.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllActiveProducts() {
        return productRepository.findByIsActiveTrue();
    }

    public Product getProductById(Integer productId) {
        return productRepository.findById(productId).orElse(null);
    }
}
