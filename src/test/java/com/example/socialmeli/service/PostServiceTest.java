package com.example.socialmeli.service;

import com.example.socialmeli.dto.FollowedPostsDto;
import com.example.socialmeli.dto.PostRequestDto;
import com.example.socialmeli.dto.PostResponseDto;
import com.example.socialmeli.dto.ProductDto;
import com.example.socialmeli.dto.PromoCountDto;
import com.example.socialmeli.dto.PromoPostsListDto;
import com.example.socialmeli.exception.BadRequestException;
import com.example.socialmeli.exception.NotFoundException;
import com.example.socialmeli.model.Post;
import com.example.socialmeli.model.Product;
import com.example.socialmeli.model.User;
import com.example.socialmeli.repository.PostRepository;
import com.example.socialmeli.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostService postService;

    private final User seller = new User(10, "vendedor1", true);
    private final User buyer = new User(1, "usuario1", false);

    // ---------- helpers para armar datos ----------

    private PostRequestDto request(Boolean hasPromo, Double discount) {
        ProductDto product = new ProductDto(1, "Silla Gamer", "Gamer", "Racer", "Red & Black", "Special Edition");
        return new PostRequestDto(10, LocalDate.now(), product, 100, 1500.50, hasPromo, discount);
    }

    private Post post(int id, int userId, LocalDate date, boolean promo) {
        Product product = new Product(id, "Producto " + id, "Gamer", "Racer", "Rojo", "Nota");
        return new Post(id, userId, date, product, 100, 1500.50, promo, promo ? 0.25 : 0.0);
    }

    private Post savedPost() {
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        return captor.getValue();
    }

    // ---------------- US-0005 createPost ----------------

    @Test
    @DisplayName("createPost: guarda una publicación normal (sin promo)")
    void createPost_ok() {
        when(userRepository.findById(10)).thenReturn(Optional.of(seller));
        when(postRepository.nextId()).thenReturn(7);

        postService.createPost(request(null, null));

        Post saved = savedPost();
        assertThat(saved.postId()).isEqualTo(7);
        assertThat(saved.userId()).isEqualTo(10);
        assertThat(saved.product().productName()).isEqualTo("Silla Gamer");
        assertThat(saved.hasPromo()).isFalse();
        assertThat(saved.discount()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("createPost: ignora has_promo/discount aunque vengan en el request")
    void createPost_ignoresPromoFields() {
        when(userRepository.findById(10)).thenReturn(Optional.of(seller));
        when(postRepository.nextId()).thenReturn(1);

        postService.createPost(request(true, 0.5));

        assertThat(savedPost().hasPromo()).isFalse();
    }

    @Test
    @DisplayName("createPost: usuario inexistente -> NotFound y no guarda")
    void createPost_userNotFound() {
        when(userRepository.findById(10)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(request(null, null)))
                .isInstanceOf(NotFoundException.class);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("createPost: si no es vendedor -> BadRequest y no guarda")
    void createPost_notSeller() {
        when(userRepository.findById(10)).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> postService.createPost(request(null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no es vendedor");
        verify(postRepository, never()).save(any(Post.class));
    }

    // ---------------- US-0010 createPromoPost ----------------

    @Test
    @DisplayName("createPromoPost: guarda la publicación con promo y descuento")
    void createPromoPost_ok() {
        when(userRepository.findById(10)).thenReturn(Optional.of(seller));
        when(postRepository.nextId()).thenReturn(3);

        postService.createPromoPost(request(true, 0.25));

        Post saved = savedPost();
        assertThat(saved.hasPromo()).isTrue();
        assertThat(saved.discount()).isEqualTo(0.25);
    }

    @Test
    @DisplayName("createPromoPost: has_promo=false -> BadRequest")
    void createPromoPost_hasPromoFalse() {
        when(userRepository.findById(10)).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> postService.createPromoPost(request(false, 0.25)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("has_promo");
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("createPromoPost: has_promo ausente -> BadRequest")
    void createPromoPost_hasPromoNull() {
        when(userRepository.findById(10)).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> postService.createPromoPost(request(null, 0.25)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("createPromoPost: discount ausente -> BadRequest")
    void createPromoPost_discountNull() {
        when(userRepository.findById(10)).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> postService.createPromoPost(request(true, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("discount");
    }

    @ParameterizedTest(name = "discount = {0} -> BadRequest")
    @ValueSource(doubles = {0.0, -0.5, 1.01})
    @DisplayName("createPromoPost: discount fuera de (0, 1] -> BadRequest")
    void createPromoPost_discountOutOfRange(double discount) {
        when(userRepository.findById(10)).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> postService.createPromoPost(request(true, discount)))
                .isInstanceOf(BadRequestException.class);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("createPromoPost: discount = 1.0 (100%) es válido")
    void createPromoPost_discountUpperBound() {
        when(userRepository.findById(10)).thenReturn(Optional.of(seller));
        when(postRepository.nextId()).thenReturn(1);

        postService.createPromoPost(request(true, 1.0));

        assertThat(savedPost().discount()).isEqualTo(1.0);
    }

    // ---------------- US-0006 + US-0009 feed ----------------

    private void stubFeed() {
        LocalDate today = LocalDate.now();
        buyer.getFollowedIds().addAll(Set.of(10, 20));
        Post recent = post(1, 10, today, true);
        Post mid = post(2, 20, today.minusDays(7), false);
        Post boundary = post(3, 10, today.minusWeeks(2), false);   // justo 14 días: entra
        Post old = post(4, 20, today.minusDays(15), false);        // 15 días: NO entra

        when(userRepository.findById(1)).thenReturn(Optional.of(buyer));
        when(postRepository.findByUserIds(anyCollection())).thenReturn(List.of(mid, old, recent, boundary));
    }

    @Test
    @DisplayName("getFollowedPosts: por defecto más recientes primero y excluye > 2 semanas")
    void getFollowedPosts_defaultOrder() {
        stubFeed();

        FollowedPostsDto result = postService.getFollowedPosts(1, null);

        assertThat(result.userId()).isEqualTo(1);
        assertThat(result.posts()).extracting(PostResponseDto::postId).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("getFollowedPosts: date_desc = más recientes primero")
    void getFollowedPosts_dateDesc() {
        stubFeed();

        assertThat(postService.getFollowedPosts(1, "date_desc").posts())
                .extracting(PostResponseDto::postId).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("getFollowedPosts: date_asc = más antiguas primero")
    void getFollowedPosts_dateAsc() {
        stubFeed();

        assertThat(postService.getFollowedPosts(1, "date_asc").posts())
                .extracting(PostResponseDto::postId).containsExactly(3, 2, 1);
    }

    @Test
    @DisplayName("getFollowedPosts: has_promo/discount solo aparecen en posts con promo")
    void getFollowedPosts_promoFieldsOnlyWhenPromo() {
        stubFeed();

        List<PostResponseDto> posts = postService.getFollowedPosts(1, null).posts();

        assertThat(posts.get(0).hasPromo()).isTrue();
        assertThat(posts.get(0).discount()).isEqualTo(0.25);
        assertThat(posts.get(1).hasPromo()).isNull();
        assertThat(posts.get(1).discount()).isNull();
    }

    @Test
    @DisplayName("getFollowedPosts: order inválido -> BadRequest")
    void getFollowedPosts_invalidOrder() {
        when(userRepository.findById(1)).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> postService.getFollowedPosts(1, "name_asc"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("getFollowedPosts: usuario inexistente -> NotFound")
    void getFollowedPosts_userNotFound() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getFollowedPosts(99, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("getFollowedPosts: si no sigue a nadie devuelve lista vacía")
    void getFollowedPosts_noFollowed() {
        when(userRepository.findById(1)).thenReturn(Optional.of(buyer));
        when(postRepository.findByUserIds(anyCollection())).thenReturn(List.of());

        assertThat(postService.getFollowedPosts(1, null).posts()).isEmpty();
    }

    // ---------------- US-0011 count promo ----------------

    @Test
    @DisplayName("countPromoPosts: cuenta solo las publicaciones en promoción")
    void countPromoPosts_ok() {
        LocalDate today = LocalDate.now();
        when(userRepository.findById(10)).thenReturn(Optional.of(seller));
        when(postRepository.findByUserId(10)).thenReturn(List.of(
                post(1, 10, today, true), post(2, 10, today, false), post(3, 10, today, true)));

        PromoCountDto result = postService.countPromoPosts(10);

        assertThat(result.userName()).isEqualTo("vendedor1");
        assertThat(result.promoProductsCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("countPromoPosts: si no es vendedor -> BadRequest")
    void countPromoPosts_notSeller() {
        when(userRepository.findById(1)).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> postService.countPromoPosts(1))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("countPromoPosts: usuario inexistente -> NotFound")
    void countPromoPosts_userNotFound() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.countPromoPosts(99))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------------- US-0012 list promo ----------------

    @Test
    @DisplayName("getPromoPosts: devuelve solo promos, más recientes primero")
    void getPromoPosts_ok() {
        LocalDate today = LocalDate.now();
        when(userRepository.findById(10)).thenReturn(Optional.of(seller));
        when(postRepository.findByUserId(10)).thenReturn(List.of(
                post(1, 10, today.minusDays(5), true),
                post(2, 10, today, true),
                post(3, 10, today, false)));

        PromoPostsListDto result = postService.getPromoPosts(10);

        assertThat(result.userName()).isEqualTo("vendedor1");
        assertThat(result.posts()).extracting(PostResponseDto::postId).containsExactly(2, 1);
        assertThat(result.posts()).allMatch(p -> Boolean.TRUE.equals(p.hasPromo()));
    }

    @Test
    @DisplayName("getPromoPosts: si no es vendedor -> BadRequest")
    void getPromoPosts_notSeller() {
        when(userRepository.findById(1)).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> postService.getPromoPosts(1))
                .isInstanceOf(BadRequestException.class);
    }
}