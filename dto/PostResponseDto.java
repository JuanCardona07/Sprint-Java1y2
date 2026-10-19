package com.example.socialmeli.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostResponseDto(
        @JsonProperty("user_id") int userId,
        @JsonProperty("post_id") int postId,
        @JsonFormat(pattern = "dd-MM-yyyy") LocalDate date,
        ProductDto product,
        int category,
        double price,
        @JsonProperty("has_promo") Boolean hasPromo,
        Double discount) {}
