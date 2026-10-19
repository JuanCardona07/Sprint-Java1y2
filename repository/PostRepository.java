package com.example.socialmeli.repository;

import com.example.socialmeli.model.Post;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class PostRepository {

    private final Map<Integer, Post> posts = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(0);

    // Genera el siguiente post_id de forma segura entre hilos
    public int nextId() {
        return sequence.incrementAndGet();
    }

    public Post save(Post post) {
        posts.put(post.postId(), post);
        return post;
    }

    public List<Post> findByUserIds(Collection<Integer> userIds) {
        return posts.values().stream().filter(p -> userIds.contains(p.userId())).toList();
    }

    public List<Post> findByUserId(int userId) {
        return posts.values().stream().filter(p -> p.userId() == userId).toList();
    }
}
