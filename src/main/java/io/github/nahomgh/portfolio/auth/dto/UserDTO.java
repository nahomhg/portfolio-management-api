package io.github.nahomgh.portfolio.auth.dto;

import io.github.nahomgh.portfolio.auth.domain.User;

public record UserDTO(Long userId, String email, String username) {
    public UserDTO(User user){
        this(user.getId(), user.getEmail(), user.getUsername());
    }
}
