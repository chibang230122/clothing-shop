
package com.example.clothingshop;

import com.example.clothingshop.entity.Product;
import com.example.clothingshop.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ProductRepositoryTests {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void testSearchProducts() {
        List<Product> products = productRepository.searchProducts(
                "ao",
                null,
                new BigDecimal("100000"),
                new BigDecimal("500000")
        );

        assertNotNull(products);

        System.out.println("So san pham tim duoc: " + products.size());

        products.forEach(product ->
                System.out.println(
                        product.getProductName()
                                + " - "
                                + product.getBasePrice()
                )
        );
    }
}
