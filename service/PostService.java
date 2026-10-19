package com.example.socialmeli.service;

import com.example.socialmeli.dto.*;
import com.example.socialmeli.exception.BadRequestException;
import com.example.socialmeli.exception.NotFoundException;
import com.example.socialmeli.model.Post;
import com.example.socialmeli.model.Product;
import com.example.socialmeli.model.User;
import com.example.socialmeli.repository.PostRepository;
import com.example.socialmeli.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    // US-0005
    public void createPost(PostRequestDto dto) {
        getSellerOrThrow(dto.userId());
        postRepository.save(buildPost(dto, false, 0.0));
    }

    // US-0010
    public void createPromoPost(PostRequestDto dto) {
        getSellerOrThrow(dto.userId());
        if (!Boolean.TRUE.equals(dto.hasPromo())) {
            throw new BadRequestException("has_promo debe ser true para una publicación en promoción");
        }
        if (dto.discount() == null || dto.discount() <= 0 || dto.discount() > 1) {
            throw new BadRequestException("discount debe ser mayor a 0 y menor o igual a 1 (ej: 0.25 = 25%)");
        }
        postRepository.save(buildPost(dto, true, dto.discount()));
    }

    // US-0006 + US-0009
    public FollowedPostsDto getFollowedPosts(int userId, String order) {
        User user = getUserOrThrow(userId);
        LocalDate twoWeeksAgo = LocalDate.now().minusWeeks(2);

        List<PostResponseDto> posts = postRepository.findByUserIds(user.getFollowedIds()).stream()
                .filter(p -> !p.date().isBefore(twoWeeksAgo))
                .sorted(dateComparator(order))
                .map(this::toResponse)
                .toList();
        return new FollowedPostsDto(user.getId(), posts);
    }

    // US-0011
    public PromoCountDto countPromoPosts(int userId) {
        User seller = getSellerOrThrow(userId);
        int count = (int) postRepository.findByUserId(userId).stream().filter(Post::hasPromo).count();
        return new PromoCountDto(seller.getId(), seller.getName(), count);
    }

    @Test
    @DisplayName("US-0011: no mezcla las promos de distintos vendedores")
    void us0011_promoCount_onlyOwnPosts() throws Exception {
        publishPromo(234, LocalDate.now(), 1, "Teclado RGB");
        publishPromo(6932, LocalDate.now(), 2, "Mouse RGB");

        mockMvc.perform(get("/products/promo-post/count").param("user_id", "234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promo_products_count", is(1)));
    }

    // US-0012 (Bonus)
    public PromoPostsListDto getPromoPosts(int userId) {
        User seller = getSellerOrThrow(userId);
        List<PostResponseDto> posts = postRepository.findByUserId(userId).stream()
                .filter(Post::hasPromo)
                .sorted(Comparator.comparing(Post::date).reversed())
                .map(this::toResponse)
                .toList();
        return new PromoPostsListDto(seller.getId(), seller.getName(), posts);
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

    private Post buildPost(PostRequestDto dto, boolean hasPromo, double discount) {
        ProductDto p = dto.product();
        Product product = new Product(p.productId(), p.productName(), p.type(),
                p.brand(), p.color(), p.notes());
        return new Post(postRepository.nextId(), dto.userId(), dto.date(), product,
                dto.category(), dto.price(), hasPromo, discount);
    }

    private Comparator<Post> dateComparator(String order) {
        if (order == null) {
            return Comparator.comparing(Post::date).reversed(); // por defecto: más recientes primero
        }
        return switch (order) {
            case "date_asc" -> Comparator.comparing(Post::date);
            case "date_desc" -> Comparator.comparing(Post::date).reversed();
            default -> throw new BadRequestException("Orden inválido: use date_asc o date_desc");
        };
    }

    private PostResponseDto toResponse(Post post) {
        Product pr = post.product();
        ProductDto productDto = new ProductDto(pr.productId(), pr.productName(), pr.type(),
                pr.brand(), pr.color(), pr.notes());
        // Si no es promo, se envía null para que Jackson omita has_promo y discount
        return new PostResponseDto(post.userId(), post.postId(), post.date(), productDto,
                post.category(), post.price(),
                post.hasPromo() ? Boolean.TRUE : null,
                post.hasPromo() ? post.discount() : null);
    }
}