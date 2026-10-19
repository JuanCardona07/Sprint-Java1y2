package com.example.socialmeli.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Datos semilla (UserRepository):
 *   Vendedores: 234 vendedor1 | 6932 vendedor2 | 6631 vendedor3 | 123 vendedor4
 *   Compradores: 4698 usuario1 | 1536 usuario2 | 2236 usuario3
 */
@SpringBootTest
@AutoConfigureMockMvc
// Los datos viven en memoria: se recarga el contexto después de cada test para que sean independientes
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SocialMeliIntegrationTest {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final String PROMO_25 = ", \"has_promo\": true, \"discount\": 0.25";

    @Autowired
    private MockMvc mockMvc;

    // ======================= helpers =======================

    private void follow(int userId, int sellerId) throws Exception {
        mockMvc.perform(post("/users/{u}/follow/{s}", userId, sellerId))
                .andExpect(status().isOk());
    }

    private String body(int userId, LocalDate date, int productId, String productName, String extraFields) {
        return """
                {"user_id": %d, "date": "%s",
                 "product": {"product_id": %d, "product_name": "%s", "type": "Gamer", "brand": "Racer",
                             "color": "Red & Black", "notes": "Special Edition"},
                 "category": 100, "price": 1500.50 %s}
                """.formatted(userId, date.format(FMT), productId, productName, extraFields);
    }

    private void publish(int userId, LocalDate date, int productId, String name) throws Exception {
        mockMvc.perform(post("/products/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(userId, date, productId, name, "")))
                .andExpect(status().isOk());
    }

    private void publishPromo(int userId, LocalDate date, int productId, String name) throws Exception {
        mockMvc.perform(post("/products/promo-post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(userId, date, productId, name, PROMO_25)))
                .andExpect(status().isCreated());
    }

    // ======================= US-0001 Follow =======================

    @Test
    @DisplayName("US-0001: follow OK -> 200 y el vendedor aparece en 'followed'")
    void us0001_follow_ok() throws Exception {
        follow(4698, 234);

        mockMvc.perform(get("/users/4698/followed/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followed", hasSize(1)))
                .andExpect(jsonPath("$.followed[0].user_id", is(234)));
    }

    @Test
    @DisplayName("US-0001: follow repetido -> 400")
    void us0001_follow_twice() throws Exception {
        follow(4698, 234);

        mockMvc.perform(post("/users/4698/follow/234")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("US-0001: seguirse a sí mismo -> 400")
    void us0001_follow_self() throws Exception {
        mockMvc.perform(post("/users/4698/follow/4698")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("US-0001: seguir a un no vendedor -> 400")
    void us0001_follow_notSeller() throws Exception {
        mockMvc.perform(post("/users/4698/follow/1536")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("US-0001: usuario inexistente -> 404")
    void us0001_follow_userNotFound() throws Exception {
        mockMvc.perform(post("/users/9999/follow/234")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("US-0001: id no numérico -> 400")
    void us0001_follow_invalidId() throws Exception {
        mockMvc.perform(post("/users/abc/follow/234")).andExpect(status().isBadRequest());
    }

    // ======================= US-0002 Count =======================

    @Test
    @DisplayName("US-0002: cantidad de seguidores del vendedor")
    void us0002_followersCount() throws Exception {
        follow(4698, 234);
        follow(1536, 234);

        mockMvc.perform(get("/users/234/followers/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id", is(234)))
                .andExpect(jsonPath("$.user_name", is("vendedor1")))
                .andExpect(jsonPath("$.followers_count", is(2)));
    }

    @Test
    @DisplayName("US-0002: contar seguidores de un no vendedor -> 400")
    void us0002_followersCount_notSeller() throws Exception {
        mockMvc.perform(get("/users/4698/followers/count")).andExpect(status().isBadRequest());
    }

    // ======================= US-0003 Followers list =======================

    @Test
    @DisplayName("US-0003: lista de usuarios que siguen al vendedor")
    void us0003_followersList() throws Exception {
        follow(4698, 234);
        follow(1536, 234);

        mockMvc.perform(get("/users/234/followers/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_name", is("vendedor1")))
                .andExpect(jsonPath("$.followers", hasSize(2)));
    }

    // ======================= US-0004 Followed list =======================

    @Test
    @DisplayName("US-0004: lista de vendedores que sigue el usuario")
    void us0004_followedList() throws Exception {
        follow(4698, 234);
        follow(4698, 6932);

        mockMvc.perform(get("/users/4698/followed/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_name", is("usuario1")))
                .andExpect(jsonPath("$.followed", hasSize(2)));
    }

    // ======================= US-0005 Create post =======================

    @Test
    @DisplayName("US-0005: publicar OK -> 200 y aparece en el feed del seguidor")
    void us0005_createPost_ok() throws Exception {
        follow(4698, 234);
        publish(234, LocalDate.now(), 1, "Silla Gamer");

        mockMvc.perform(get("/products/followed/4698/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(1)))
                .andExpect(jsonPath("$.posts[0].product.product_name", is("Silla Gamer")))
                .andExpect(jsonPath("$.posts[0].date", is(LocalDate.now().format(FMT))));
    }

    @Test
    @DisplayName("US-0005: nombre de producto de más de 40 caracteres -> 400")
    void us0005_createPost_nameTooLong() throws Exception {
        mockMvc.perform(post("/products/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(234, LocalDate.now(), 1, "x".repeat(41), "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("US-0005: fecha futura -> 400")
    void us0005_createPost_futureDate() throws Exception {
        mockMvc.perform(post("/products/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(234, LocalDate.now().plusDays(1), 1, "Silla", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("US-0005: fecha con formato incorrecto -> 400")
    void us0005_createPost_badDateFormat() throws Exception {
        String json = """
                {"user_id": 234, "date": "2026-10-10",
                 "product": {"product_id": 1, "product_name": "Silla", "type": "Gamer", "brand": "Racer",
                             "color": "Rojo", "notes": "n"},
                 "category": 100, "price": 100.0}
                """;

        mockMvc.perform(post("/products/post").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("US-0005: sin producto -> 400")
    void us0005_createPost_missingProduct() throws Exception {
        String json = "{\"user_id\": 234, \"date\": \"" + LocalDate.now().format(FMT)
                + "\", \"category\": 100, \"price\": 100.0}";

        mockMvc.perform(post("/products/post").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("US-0005: publicar con un usuario que no es vendedor -> 400")
    void us0005_createPost_notSeller() throws Exception {
        mockMvc.perform(post("/products/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(4698, LocalDate.now(), 1, "Silla", "")))
                .andExpect(status().isBadRequest());
    }

    // ======================= US-0006 Feed =======================

    @Test
    @DisplayName("US-0006: feed trae posts de últimas 2 semanas, más recientes primero")
    void us0006_feed() throws Exception {
        follow(4698, 234);
        follow(4698, 6932);
        publish(234, LocalDate.now(), 1, "Silla Gamer");
        publish(6932, LocalDate.now().minusDays(5), 2, "Mouse");
        publish(234, LocalDate.now().minusDays(30), 3, "Monitor viejo");   // fuera de las 2 semanas

        mockMvc.perform(get("/products/followed/4698/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id", is(4698)))
                .andExpect(jsonPath("$.posts", hasSize(2)))
                .andExpect(jsonPath("$.posts[0].product.product_name", is("Silla Gamer")))
                .andExpect(jsonPath("$.posts[1].product.product_name", is("Mouse")))
                .andExpect(jsonPath("$.posts[0].has_promo").doesNotExist());
    }

    @Test
    @DisplayName("US-0006: feed no incluye posts de vendedores que no sigue")
    void us0006_feed_onlyFollowed() throws Exception {
        follow(4698, 234);
        publish(6932, LocalDate.now(), 2, "Mouse");

        mockMvc.perform(get("/products/followed/4698/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(0)));
    }

    @Test
    @DisplayName("US-0006: usuario inexistente -> 404")
    void us0006_feed_userNotFound() throws Exception {
        mockMvc.perform(get("/products/followed/9999/list")).andExpect(status().isNotFound());
    }

    // ======================= US-0007 Unfollow =======================

    @Test
    @DisplayName("US-0007: unfollow OK -> 200 y baja el contador de seguidores")
    void us0007_unfollow_ok() throws Exception {
        follow(4698, 234);

        mockMvc.perform(post("/users/4698/unfollow/234")).andExpect(status().isOk());

        mockMvc.perform(get("/users/234/followers/count"))
                .andExpect(jsonPath("$.followers_count", is(0)));
    }

    @Test
    @DisplayName("US-0007: unfollow de alguien que no sigue -> 400")
    void us0007_unfollow_notFollowing() throws Exception {
        mockMvc.perform(post("/users/4698/unfollow/234")).andExpect(status().isBadRequest());
    }

    // ======================= US-0008 Orden alfabético =======================

    @Test
    @DisplayName("US-0008: seguidores ordenados name_asc y name_desc")
    void us0008_followers_order() throws Exception {
        follow(4698, 234);   // usuario1
        follow(1536, 234);   // usuario2
        follow(2236, 234);   // usuario3

        mockMvc.perform(get("/users/234/followers/list").param("order", "name_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followers[0].user_name", is("usuario1")))
                .andExpect(jsonPath("$.followers[2].user_name", is("usuario3")));

        mockMvc.perform(get("/users/234/followers/list").param("order", "name_desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followers[0].user_name", is("usuario3")))
                .andExpect(jsonPath("$.followers[2].user_name", is("usuario1")));
    }

    @Test
    @DisplayName("US-0008: seguidos ordenados name_asc y name_desc")
    void us0008_followed_order() throws Exception {
        follow(4698, 6932);  // vendedor2
        follow(4698, 234);   // vendedor1

        mockMvc.perform(get("/users/4698/followed/list").param("order", "name_asc"))
                .andExpect(jsonPath("$.followed[0].user_name", is("vendedor1")))
                .andExpect(jsonPath("$.followed[1].user_name", is("vendedor2")));

        mockMvc.perform(get("/users/4698/followed/list").param("order", "name_desc"))
                .andExpect(jsonPath("$.followed[0].user_name", is("vendedor2")))
                .andExpect(jsonPath("$.followed[1].user_name", is("vendedor1")));
    }

    @Test
    @DisplayName("US-0008: order inválido -> 400")
    void us0008_invalidOrder() throws Exception {
        mockMvc.perform(get("/users/234/followers/list").param("order", "xyz"))
                .andExpect(status().isBadRequest());
    }

    // ======================= US-0009 Orden por fecha =======================

    @Test
    @DisplayName("US-0009: feed ordenado date_asc y date_desc")
    void us0009_feed_order() throws Exception {
        follow(4698, 234);
        publish(234, LocalDate.now().minusDays(6), 1, "Antiguo");
        publish(234, LocalDate.now().minusDays(2), 2, "Reciente");

        mockMvc.perform(get("/products/followed/4698/list").param("order", "date_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[0].product.product_name", is("Antiguo")))
                .andExpect(jsonPath("$.posts[1].product.product_name", is("Reciente")));

        mockMvc.perform(get("/products/followed/4698/list").param("order", "date_desc"))
                .andExpect(jsonPath("$.posts[0].product.product_name", is("Reciente")))
                .andExpect(jsonPath("$.posts[1].product.product_name", is("Antiguo")));
    }

    @Test
    @DisplayName("US-0009: order inválido en el feed -> 400")
    void us0009_invalidOrder() throws Exception {
        follow(4698, 234);

        mockMvc.perform(get("/products/followed/4698/list").param("order", "name_asc"))
                .andExpect(status().isBadRequest());
    }

    // ======================= US-0010 Promo post =======================

    @Test
    @DisplayName("US-0010: publicar en promoción -> 201 y el feed muestra has_promo y discount")
    void us0010_promoPost_ok() throws Exception {
        follow(4698, 234);
        publishPromo(234, LocalDate.now(), 2, "Teclado RGB");

        mockMvc.perform(get("/products/followed/4698/list"))
                .andExpect(jsonPath("$.posts[0].has_promo", is(true)))
                .andExpect(jsonPath("$.posts[0].discount", is(0.25)));
    }

    @Test
    @DisplayName("US-0010: descuento mayor a 1 -> 400")
    void us0010_promoPost_discountTooHigh() throws Exception {
        mockMvc.perform(post("/products/promo-post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(234, LocalDate.now(), 2, "Teclado",
                                ", \"has_promo\": true, \"discount\": 1.5")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("US-0010: has_promo en false -> 400")
    void us0010_promoPost_hasPromoFalse() throws Exception {
        mockMvc.perform(post("/products/promo-post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(234, LocalDate.now(), 2, "Teclado",
                                ", \"has_promo\": false, \"discount\": 0.25")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("US-0010: sin discount -> 400")
    void us0010_promoPost_missingDiscount() throws Exception {
        mockMvc.perform(post("/products/promo-post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(234, LocalDate.now(), 2, "Teclado", ", \"has_promo\": true")))
                .andExpect(status().isBadRequest());
    }

    // ======================= US-0011 Count promo =======================

    @Test
    @DisplayName("US-0011: cuenta solo los productos en promoción del vendedor")
    void us0011_promoCount() throws Exception {
        publish(234, LocalDate.now(), 1, "Silla normal");
        publishPromo(234, LocalDate.now(), 2, "Teclado RGB");
        publishPromo(234, LocalDate.now(), 3, "Mouse RGB");

        mockMvc.perform(get("/products/promo-post/count").param("user_id", "234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id", is(234)))
                .andExpect(jsonPath("$.user_name", is("vendedor1")))
                .andExpect(jsonPath("$.promo_products_count", is(2)));
    }

    @Test
    @DisplayName("US-0011: sin parámetro user_id -> 400")
    void us0011_promoCount_missingParam() throws Exception {
        mockMvc.perform(get("/products/promo-post/count")).andExpect(status().isBadRequest());
    }

    // ======================= US-0012 List promo (bonus) =======================

    @Test
    @DisplayName("US-0012: lista solo los productos en promoción del vendedor")
    void us0012_promoList() throws Exception {
        publish(234, LocalDate.now(), 1, "Silla normal");
        publishPromo(234, LocalDate.now(), 2, "Teclado RGB");

        mockMvc.perform(get("/products/promo-post/list").param("user_id", "234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_name", is("vendedor1")))
                .andExpect(jsonPath("$.posts", hasSize(1)))
                .andExpect(jsonPath("$.posts[0].product.product_name", is("Teclado RGB")))
                .andExpect(jsonPath("$.posts[0].has_promo", is(true)))
                .andExpect(jsonPath("$.posts[0].discount", is(0.25)));
    }

    @Test
    @DisplayName("US-0012: vendedor inexistente -> 404")
    void us0012_promoList_userNotFound() throws Exception {
        mockMvc.perform(get("/products/promo-post/list").param("user_id", "9999"))
                .andExpect(status().isNotFound());
    }
}