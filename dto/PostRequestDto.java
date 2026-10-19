package com.example.socialmeli.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record PostRequestDto(
        @JsonProperty("user_id") @NotNull @Positive Integer userId,
        @JsonFormat(pattern = "dd-MM-yyyy") @NotNull @PastOrPresent LocalDate date,
        @NotNull @Valid ProductDto product,
        @NotNull @Positive Integer category,
        @NotNull @Positive @DecimalMax("10000000.0") Double price,
        @JsonProperty("has_promo") Boolean hasPromo,
        @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("1.0") Double discount) {}