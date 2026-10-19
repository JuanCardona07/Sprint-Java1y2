package com.example.socialmeli.service;

import com.example.socialmeli.dto.*;
import com.example.socialmeli.exception.BadRequestException;
import com.example.socialmeli.exception.NotFoundException;
import com.example.socialmeli.model.User;
import com.example.socialmeli.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // US-0001
    public void follow(int userId, int userIdToFollow) {
        if (userId == userIdToFollow) {
            throw new BadRequestException("Un usuario no puede seguirse a sí mismo");
        }
        User user = getUserOrThrow(userId);
        User seller = getSellerOrThrow(userIdToFollow);
        // add() devuelve false si ya existía
        if (!user.getFollowedIds().add(seller.getId())) {
            throw new BadRequestException("El usuario " + userId + " ya sigue al vendedor " + userIdToFollow);
        }
    }

    // US-0007
    public void unfollow(int userId, int userIdToUnfollow) {
        User user = getUserOrThrow(userId);
        getSellerOrThrow(userIdToUnfollow);
        // remove() devuelve false si no estaba
        if (!user.getFollowedIds().remove(userIdToUnfollow)) {
            throw new BadRequestException("El usuario " + userId + " no sigue al vendedor " + userIdToUnfollow);
        }
    }

    // US-0002
    public FollowersCountDto countFollowers(int userId) {
        User seller = getSellerOrThrow(userId);
        int count = userRepository.findFollowersOf(userId).size();
        return new FollowersCountDto(seller.getId(), seller.getName(), count);
    }

    // US-0003 + US-0008
    public FollowersListDto getFollowers(int userId, String order) {
        User seller = getSellerOrThrow(userId);
        List<UserDto> followers = userRepository.findFollowersOf(userId).stream()
                .sorted(nameComparator(order))
                .map(this::toDto)
                .toList();
        return new FollowersListDto(seller.getId(), seller.getName(), followers);
    }

    // US-0004 + US-0008
    public FollowedListDto getFollowed(int userId, String order) {
        User user = getUserOrThrow(userId);
        List<UserDto> followed = userRepository.findAllById(user.getFollowedIds()).stream()
                .sorted(nameComparator(order))
                .map(this::toDto)
                .toList();
        return new FollowedListDto(user.getId(), user.getName(), followed);
    }

    // ---------- helpers ----------

    private User getUserOrThrow(int id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario " + id + " no encontrado"));
    }

    private User getSellerOrThrow(int id) {
        User user = getUserOrThrow(id);
        if (!user.isSeller()) {
            throw new BadRequestException("El usuario " + id + " no es vendedor");
        }
        return user;
    }

    private Comparator<User> nameComparator(String order) {
        if (order == null) {
            return (a, b) -> 0; // sin orden: se conserva el original
        }
        Comparator<User> byName = Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER);
        return switch (order) {
            case "name_asc" -> byName;
            case "name_desc" -> byName.reversed();
            default -> throw new BadRequestException("Orden inválido: use name_asc o name_desc");
        };
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getName());
    }
}