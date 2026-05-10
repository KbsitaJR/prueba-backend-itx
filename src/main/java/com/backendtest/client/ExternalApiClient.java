package com.backendtest.client;

import com.backendtest.dto.ProductDetail;
import com.backendtest.exception.ProductNotFoundException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ExternalApiClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiClient.class);
    private final RestClient restClient;

    public ExternalApiClient(
            @Value("${external-api.base-url}") String baseUrl,
            @Value("${external-api.connect-timeout}") Duration connectTimeout,
            @Value("${external-api.read-timeout}") Duration readTimeout
    ) {
        var factory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build()
        );
        factory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .build();
    }

    public List<String> fetchSimilarIds(String productId) {
        log.info("fetching_similar_ids productId={}", productId);
        return restClient.get()
            .uri("/product/{productId}/similarids", productId)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                throw new ProductNotFoundException(productId);
            })
            .body(new SimilarIdsList());
    }

    public Optional<ProductDetail> fetchProductDetail(String productId) {
        try {
            log.debug("fetching_product_detail productId={}", productId);
            ProductDetail detail = restClient.get()
                .uri("/product/{productId}", productId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {})
                .body(ProductDetail.class);
            log.debug("fetched_product_detail productId={}", productId);
            return Optional.ofNullable(detail);
        } catch (Exception ex) {
            log.warn("fetch_product_detail_failed productId={} error={}", productId, ex.getMessage());
            return Optional.empty();
        }
    }
}
