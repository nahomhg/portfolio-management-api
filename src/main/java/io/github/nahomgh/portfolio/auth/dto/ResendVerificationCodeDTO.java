package io.github.nahomgh.portfolio.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationCodeDTO(
        @NotBlank @Email
        String userEmail) {
}
