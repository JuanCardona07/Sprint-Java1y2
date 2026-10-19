package com.example.socialmeli.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserDto(
        @JsonProperty("user_id") int userId,
        @JsonProperty("user_name") String userName)  {}
