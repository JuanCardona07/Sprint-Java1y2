package com.example.socialmeli.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductDto(
        @JsonProperty("product_id") @NotNull @Positive Integer productId,
        @JsonProperty("product_name") @NotBlank @Size(max = 40) String productName,
        @NotBlank @Size(max = 15) String type,
        @NotBlank @Size(max = 25) String brand,
        @NotBlank @Size(max = 15) String color,
        @Size(max = 80) String notes) {}