package com.example.socialmeli.model;

import java.time.LocalDate;

public record Post (int postId, int userId, LocalDate date, Product product,
                    int category, double price, boolean hasPromo, double discount) {}
