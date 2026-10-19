package com.example.socialmeli.controller;

import com.example.socialmeli.dto.FollowedListDto;
import com.example.socialmeli.dto.FollowersCountDto;
import com.example.socialmeli.dto.FollowersListDto;
import com.example.socialmeli.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // US-0001: POST /users/{userId}/follow/{userIdToFollow}
    @PostMapping("/{userId}/follow/{userIdToFollow}")
    public ResponseEntity<Void> follow(@PathVariable int userId, @PathVariable int userIdToFollow) {
        userService.follow(userId, userIdToFollow);
        return ResponseEntity.ok().build();
    }

    // US-0002: GET /users/{userId}/followers/count
    @GetMapping("/{userId}/followers/count")
    public ResponseEntity<FollowersCountDto> countFollowers(@PathVariable int userId) {
        return ResponseEntity.ok(userService.countFollowers(userId));
    }

    // US-0003 + US-0008: GET /users/{userId}/followers/list?order=name_asc
    @GetMapping("/{userId}/followers/list")
    public ResponseEntity<FollowersListDto> getFollowers(@PathVariable int userId,
                                                         @RequestParam(required = false) String order) {
        return ResponseEntity.ok(userService.getFollowers(userId, order));
    }

    // US-0004 + US-0008: GET /users/{userId}/followed/list?order=name_desc
    @GetMapping("/{userId}/followed/list")
    public ResponseEntity<FollowedListDto> getFollowed(@PathVariable int userId,
                                                       @RequestParam(required = false) String order) {
        return ResponseEntity.ok(userService.getFollowed(userId, order));
    }

    // US-0007: POST /users/{userId}/unfollow/{userIdToUnfollow}
    @PostMapping("/{userId}/unfollow/{userIdToUnfollow}")
    public ResponseEntity<Void> unfollow(@PathVariable int userId, @PathVariable int userIdToUnfollow) {
        userService.unfollow(userId, userIdToUnfollow);
        return ResponseEntity.ok().build();
    }
}
