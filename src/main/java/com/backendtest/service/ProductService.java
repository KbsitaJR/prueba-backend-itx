package com.backendtest.service;

import com.backendtest.client.ExternalApiClient;
import com.backendtest.dto.ProductDetail;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ExternalApiClient apiClient;
    private final long perProductTimeoutMs;

    public ProductService(
            ExternalApiClient apiClient,
            @Value("${app.per-product-timeout:3000}") long perProductTimeoutMs
    ) {
        this.apiClient = apiClient;
        this.perProductTimeoutMs = perProductTimeoutMs;
    }

    public List<ProductDetail> getSimilarProducts(String productId) {
        List<String> similarIds = apiClient.fetchSimilarIds(productId);

        if (similarIds.isEmpty()) {
            log.info("no_similar_products productId={}", productId);
            return Collections.emptyList();
        }

        log.info("fetching_details_for_similar productId={} similarCount={}", productId, similarIds.size());

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<ProductDetail>> futures = similarIds.stream()
                .map(id -> CompletableFuture.supplyAsync(
                        () -> apiClient.fetchProductDetail(id).orElse(null), executor)
                    .orTimeout(perProductTimeoutMs, TimeUnit.MILLISECONDS)
                    .exceptionally(ex -> {
                        log.warn("timeout_or_error productId={} error={}", id, ex.getMessage());
                        return null;
                    }))
                .toList();

            return futures.stream()
                .map(CompletableFuture::join)
                .filter(detail -> detail != null)
                .toList();
        }
    }
}
