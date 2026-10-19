package com.example.socialmeli.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FollowedPostsDto(
        @JsonProperty("user_id") int userId,
        List<PostResponseDto> posts) {}