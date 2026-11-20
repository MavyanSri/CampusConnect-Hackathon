package com.mediflow.backend.dto;

public record SpecializationResponse(
        Long id,
        String name,
        String description,
        boolean active
) {
}