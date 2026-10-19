package com.example.socialmeli.model;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class User {
    private final int id;
    private final String name;
    private final boolean seller;
    private final Set<Integer> followedIds = ConcurrentHashMap.newKeySet();

    public User(int id, String name, boolean seller) {
        this.id = id;
        this.name = name;
        this.seller = seller;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isSeller() {
        return seller;
    }

    public Set<Integer> getFollowedIds() {
        return followedIds;
    }
}
