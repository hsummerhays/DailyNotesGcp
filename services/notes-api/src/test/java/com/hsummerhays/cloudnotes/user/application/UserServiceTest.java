package com.hsummerhays.cloudnotes.user.application;

import com.hsummerhays.cloudnotes.security.JwtService;
import com.hsummerhays.cloudnotes.user.domain.User;
import com.hsummerhays.cloudnotes.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthenticationManager authenticationManager;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        authenticationManager = mock(AuthenticationManager.class);
        userService = new UserService(userRepository, passwordEncoder, jwtService, authenticationManager);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registerUser_newEmail_hashesPasswordAndAssignsUserRole() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plaintext")).thenReturn("hashed");

        User user = userService.registerUser("new@example.com", "plaintext", "New User");

        assertThat(user.getPasswordHash()).isEqualTo("hashed");
        assertThat(user.getRoles()).containsExactly("ROLE_USER");
    }

    @Test
    void registerUser_duplicateEmail_throwsAndDoesNotSave() {
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(
                new User(UUID.randomUUID(), "existing@example.com", "hash", "Name", Set.of("ROLE_USER"))));

        assertThatThrownBy(() -> userService.registerUser("existing@example.com", "pw", "Name"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticate_validCredentials_returnsGeneratedToken() {
        User user = new User(UUID.randomUUID(), "user@example.com", "hash", "Name", Set.of("ROLE_USER"));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("signed-jwt");

        UserService.AuthenticationResult result = userService.authenticate("user@example.com", "pw");

        assertThat(result.token()).isEqualTo("signed-jwt");
        assertThat(result.user()).isSameAs(user);
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void authenticate_badCredentials_propagatesAuthenticationException() {
        doThrow(new BadCredentialsException("bad creds")).when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> userService.authenticate("user@example.com", "wrong"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void getUserByEmail_unknownEmail_throws() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail("ghost@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
