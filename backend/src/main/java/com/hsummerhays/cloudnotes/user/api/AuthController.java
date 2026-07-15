package com.hsummerhays.cloudnotes.user.api;

import com.hsummerhays.cloudnotes.user.application.UserService;
import com.hsummerhays.cloudnotes.user.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerUser(request.email(), request.password(), request.displayName());
        String token = userService.authenticate(request.email(), request.password());
        return new AuthResponse(token, user.getEmail(), user.getDisplayName());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userService.getUserByEmail(request.email());
        String token = userService.authenticate(request.email(), request.password());
        return new AuthResponse(token, user.getEmail(), user.getDisplayName());
    }
}
