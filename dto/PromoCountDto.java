package com.example.socialmeli.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PromoCountDto(
        @JsonProperty("user_id") int userId,
        @JsonProperty("user_name") String userName,
        @JsonProperty("promo_products_count") int promoProductsCount) {}