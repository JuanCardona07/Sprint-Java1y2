package com.example.socialmeli.service;

import com.example.socialmeli.dto.FollowedListDto;
import com.example.socialmeli.dto.FollowersCountDto;
import com.example.socialmeli.dto.FollowersListDto;
import com.example.socialmeli.dto.UserDto;
import com.example.socialmeli.exception.BadRequestException;
import com.example.socialmeli.exception.NotFoundException;
import com.example.socialmeli.model.User;
import com.example.socialmeli.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User buyer;       // id 1, comprador
    private User seller;      // id 2, vendedor
    private User otherBuyer;  // id 3, comprador

    @BeforeEach
    void setUp() {
        buyer = new User(1, "usuario1", false);
        seller = new User(2, "vendedor1", true);
        otherBuyer = new User(3, "usuario2", false);
    }

    // ---------------- US-0001 follow ----------------

    @Test
    @DisplayName("follow: agrega al vendedor a la lista de seguidos")
    void follow_ok() {
        when(userRepository.findById(1)).thenReturn(Optional.of(buyer));
        when(userRepository.findById(2)).thenReturn(Optional.of(seller));

        userService.follow(1, 2);

        assertThat(buyer.getFollowedIds()).containsExactly(2);
    }

    @Test
    @DisplayName("follow: un usuario no puede seguirse a sí mismo")
    void follow_self() {
        assertThatThrownBy(() -> userService.follow(1, 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("sí mismo");
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("follow: usuario que sigue no existe -> NotFound")
    void follow_userNotFound() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.follow(99, 2))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("follow: vendedor a seguir no existe -> NotFound")
    void follow_sellerNotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.of(buyer));
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.follow(1, 99))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("follow: no se puede seguir a alguien que no es vendedor")
    void follow_targetNotSeller() {
        when(userRepository.findById(1)).thenReturn(Optional.of(buyer));
        when(userRepository.findById(3)).thenReturn(Optional.of(otherBuyer));

        assertThatThrownBy(() -> userService.follow(1, 3))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no es vendedor");
    }

    @Test
    @DisplayName("follow: seguir dos veces al mismo vendedor -> BadRequest")
    void follow_alreadyFollowing() {
        buyer.getFollowedIds().add(2);
        when(userRepository.findById(1)).thenReturn(Optional.of(buyer));
        when(userRepository.findById(2)).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> userService.follow(1, 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ya sigue");
    }

    // ---------------- US-0007 unfollow ----------------

    @Test
    @DisplayName("unfollow: quita al vendedor de la lista de seguidos")
    void unfollow_ok() {
        buyer.getFollowedIds().add(2);
        when(userRepository.findById(1)).thenReturn(Optional.of(buyer));
        when(userRepository.findById(2)).thenReturn(Optional.of(seller));

        userService.unfollow(1, 2);

        assertThat(buyer.getFollowedIds()).isEmpty();
    }

    @Test
    @DisplayName("unfollow: si no lo seguía -> BadRequest")
    void unfollow_notFollowing() {
        when(userRepository.findById(1)).thenReturn(Optional.of(buyer));
        when(userRepository.findById(2)).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> userService.unfollow(1, 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no sigue");
    }

    // ---------------- US-0002 count ----------------

    @Test
    @DisplayName("countFollowers: devuelve la cantidad de seguidores y el nombre")
    void countFollowers_ok() {
        when(userRepository.findById(2)).thenReturn(Optional.of(seller));
        when(userRepository.findFollowersOf(2)).thenReturn(List.of(buyer, otherBuyer));

        FollowersCountDto result = userService.countFollowers(2);

        assertThat(result.userId()).isEqualTo(2);
        assertThat(result.userName()).isEqualTo("vendedor1");
        assertThat(result.followersCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("countFollowers: si el usuario no es vendedor -> BadRequest")
    void countFollowers_notSeller() {
        when(userRepository.findById(1)).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> userService.countFollowers(1))
                .isInstanceOf(BadRequestException.class);
    }

    // ---------------- US-0003 + US-0008 followers list ----------------

    private void stubFollowers() {
        User zoe = new User(10, "zoe", false);
        User ana = new User(11, "ana", false);
        User beto = new User(12, "Beto", false);
        when(userRepository.findById(2)).thenReturn(Optional.of(seller));
        when(userRepository.findFollowersOf(2)).thenReturn(List.of(zoe, ana, beto));
    }

    @Test
    @DisplayName("getFollowers: name_asc ordena A-Z ignorando mayúsculas")
    void getFollowers_nameAsc() {
        stubFollowers();

        FollowersListDto result = userService.getFollowers(2, "name_asc");

        assertThat(result.followers()).extracting(UserDto::userName)
                .containsExactly("ana", "Beto", "zoe");
    }

    @Test
    @DisplayName("getFollowers: name_desc ordena Z-A")
    void getFollowers_nameDesc() {
        stubFollowers();

        FollowersListDto result = userService.getFollowers(2, "name_desc");

        assertThat(result.followers()).extracting(UserDto::userName)
                .containsExactly("zoe", "Beto", "ana");
    }

    @Test
    @DisplayName("getFollowers: sin order conserva el orden original")
    void getFollowers_noOrder() {
        stubFollowers();

        FollowersListDto result = userService.getFollowers(2, null);

        assertThat(result.followers()).extracting(UserDto::userName)
                .containsExactly("zoe", "ana", "Beto");
    }

    @Test
    @DisplayName("getFollowers: order inválido -> BadRequest")
    void getFollowers_invalidOrder() {
        when(userRepository.findById(2)).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> userService.getFollowers(2, "xyz"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Orden inválido");
    }

    // ---------------- US-0004 + US-0008 followed list ----------------

    @Test
    @DisplayName("getFollowed: name_asc ordena los vendedores seguidos")
    void getFollowed_nameAsc() {
        User sellerB = new User(20, "vendedorB", true);
        User sellerA = new User(21, "vendedorA", true);
        buyer.getFollowedIds().add(20);
        buyer.getFollowedIds().add(21);
        when(userRepository.findById(1)).thenReturn(Optional.of(buyer));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(sellerB, sellerA));

        FollowedListDto result = userService.getFollowed(1, "name_asc");

        assertThat(result.userName()).isEqualTo("usuario1");
        assertThat(result.followed()).extracting(UserDto::userName)
                .containsExactly("vendedorA", "vendedorB");
    }

    @Test
    @DisplayName("getFollowed: name_desc ordena Z-A")
    void getFollowed_nameDesc() {
        User sellerB = new User(20, "vendedorB", true);
        User sellerA = new User(21, "vendedorA", true);
        when(userRepository.findById(1)).thenReturn(Optional.of(buyer));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(sellerA, sellerB));

        FollowedListDto result = userService.getFollowed(1, "name_desc");

        assertThat(result.followed()).extracting(UserDto::userName)
                .containsExactly("vendedorB", "vendedorA");
    }

    @Test
    @DisplayName("getFollowed: usuario inexistente -> NotFound")
    void getFollowed_userNotFound() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getFollowed(99, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("getFollowed: order inválido -> BadRequest")
    void getFollowed_invalidOrder() {
        when(userRepository.findById(1)).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> userService.getFollowed(1, "date_asc"))
                .isInstanceOf(BadRequestException.class);
    }
}