package io.github.nahomgh.portfolio.auth.dto;

public record AuthResponseDTO(String token, long expiresIn) {
}
