package com.backendtest.controller;

import com.backendtest.dto.ProductDetail;
import com.backendtest.service.ProductService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/product/{productId}/similar")
    ResponseEntity<List<ProductDetail>> getSimilarProducts(@PathVariable String productId) {
        List<ProductDetail> products = productService.getSimilarProducts(productId);
        return ResponseEntity.ok(products);
    }
}
