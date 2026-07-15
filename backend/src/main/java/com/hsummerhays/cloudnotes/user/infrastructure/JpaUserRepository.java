package com.hsummerhays.cloudnotes.user.infrastructure;

import com.hsummerhays.cloudnotes.user.domain.User;
import com.hsummerhays.cloudnotes.user.domain.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository repository;

    public JpaUserRepository(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        return repository.save(user);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email);
    }
}
