package io.github.nahomgh.portfolio.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendVerificationCodeDTO(@NotBlank String userEmail) {
}
