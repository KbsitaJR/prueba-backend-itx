package com.backendtest.dto;

public record ProductDetail(
    String id,
    String name,
    double price,
    boolean availability
) {}
