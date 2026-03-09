package com.keybudget.user;

import com.keybudget.user.dto.UserProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserServiceImpl userServiceImpl;

    @BeforeEach
    void setUp() {
        userServiceImpl = new UserServiceImpl(userRepository);
    }

    @Test
    void upsertFromGoogle_givenNewUser_createsAndReturnsUser() {
        User saved = buildUser(1L, "j@example.com", "Justin");
        when(userRepository.findByGoogleSub("sub-123")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = userServiceImpl.upsertFromGoogle("sub-123", "j@example.com", "Justin", "https://pic");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("j@example.com");
    }

    @Test
    void upsertFromGoogle_givenExistingUser_updatesAndReturnsUser() {
        User existing = buildUser(1L, "old@example.com", "OldName");
        when(userRepository.findByGoogleSub("sub-123")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User result = userServiceImpl.upsertFromGoogle("sub-123", "new@example.com", "NewName", "https://newpic");

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getName()).isEqualTo("NewName");
    }

    @Test
    void findById_givenValidUserId_returnsUser() {
        User user = buildUser(1L, "j@example.com", "Justin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userServiceImpl.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void findById_givenUnknownUserId_throwsIllegalArgument() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userServiceImpl.findById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getProfile_givenValidUserId_returnsProfile() {
        User user = buildUser(1L, "j@example.com", "Justin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse profile = userServiceImpl.getProfile(1L);

        assertThat(profile.id()).isEqualTo(1L);
        assertThat(profile.email()).isEqualTo("j@example.com");
    }

    @Test
    void getProfile_givenUnknownUserId_throwsIllegalArgument() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userServiceImpl.getProfile(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    private User buildUser(Long id, String email, String name) {
        User user = new User() {
            @Override
            public Long getId() { return id; }
        };
        user.setGoogleSub("sub-123");
        user.setEmail(email);
        user.setName(name);
        user.setPictureUrl("https://pic");
        return user;
    }
}
