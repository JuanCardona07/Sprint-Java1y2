package com.example.socialmeli.controller;

import com.example.socialmeli.dto.FollowedPostsDto;
import com.example.socialmeli.dto.PostRequestDto;
import com.example.socialmeli.dto.PromoCountDto;
import com.example.socialmeli.dto.PromoPostsListDto;
import com.example.socialmeli.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final PostService postService;

    public ProductController(PostService postService) {
        this.postService = postService;
    }

    // US-0005: POST /products/post  -> 200
    @PostMapping("/post")
    public ResponseEntity<Void> createPost(@Valid @RequestBody PostRequestDto dto) {
        postService.createPost(dto);
        return ResponseEntity.ok().build();
    }

    // US-0006 + US-0009: GET /products/followed/{userId}/list?order=date_asc
    @GetMapping("/followed/{userId}/list")
    public ResponseEntity<FollowedPostsDto> getFollowedPosts(@PathVariable int userId,
                                                             @RequestParam(required = false) String order) {
        return ResponseEntity.ok(postService.getFollowedPosts(userId, order));
    }

    // US-0010: POST /products/promo-post  -> 201
    @PostMapping("/promo-post")
    public ResponseEntity<Void> createPromoPost(@Valid @RequestBody PostRequestDto dto) {
        postService.createPromoPost(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // US-0011: GET /products/promo-post/count?user_id=234
    @GetMapping("/promo-post/count")
    public ResponseEntity<PromoCountDto> countPromoPosts(@RequestParam("user_id") int userId) {
        return ResponseEntity.ok(postService.countPromoPosts(userId));
    }

    // US-0012 (Bonus): GET /products/promo-post/list?user_id=234
    @GetMapping("/promo-post/list")
    public ResponseEntity<PromoPostsListDto> getPromoPosts(@RequestParam("user_id") int userId) {
        return ResponseEntity.ok(postService.getPromoPosts(userId));
    }
}
