package com.example.socialmeli.repository;

import com.example.socialmeli.model.User;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class UserRepository {

    private final Map<Integer, User> users = new ConcurrentHashMap<>();

    // Datos de prueba para poder probar sin base de datos
    public UserRepository() {
        add(new User(234, "vendedor1", true));
        add(new User(6932, "vendedor2", true));
        add(new User(6631, "vendedor3", true));
        add(new User(123, "vendedor4", true));
        add(new User(4698, "usuario1", false));
        add(new User(1536, "usuario2", false));
        add(new User(2236, "usuario3", false));
    }

    private void add(User user) {
        users.put(user.getId(), user);
    }

    public Optional<User> findById(int id) {
        return Optional.ofNullable(users.get(id));
    }

    public List<User> findAllById(Collection<Integer> ids) {
        return ids.stream().map(users::get).filter(Objects::nonNull).toList();
    }

    // Todos los usuarios que tienen a sellerId en su lista de seguidos
    public List<User> findFollowersOf(int sellerId) {
        return users.values().stream()
                .filter(u -> u.getFollowedIds().contains(sellerId))
                .toList();
    }
}