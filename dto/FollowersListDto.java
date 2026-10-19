package com.example.socialmeli.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FollowersListDto(
        @JsonProperty("user_id") int userId,
        @JsonProperty("user_name") String userName,
        List<UserDto> followers) {}