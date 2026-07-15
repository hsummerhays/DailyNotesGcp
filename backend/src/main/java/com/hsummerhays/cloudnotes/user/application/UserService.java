package com.hsummerhays.cloudnotes.user.application;

import com.hsummerhays.cloudnotes.security.JwtService;
import com.hsummerhays.cloudnotes.security.SecurityUser;
import com.hsummerhays.cloudnotes.user.domain.User;
import com.hsummerhays.cloudnotes.user.domain.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public User registerUser(String email, String password, String displayName) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }

        String passwordHash = passwordEncoder.encode(password);
        User user = new User(UUID.randomUUID(), email, passwordHash, displayName, Set.of("ROLE_USER"));
        return userRepository.save(user);
    }

    public String authenticate(String email, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserDetails userDetails = new SecurityUser(user);
        return jwtService.generateToken(userDetails);
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }
}
