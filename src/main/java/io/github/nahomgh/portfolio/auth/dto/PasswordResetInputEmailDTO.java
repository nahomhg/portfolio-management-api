package io.github.nahomgh.portfolio.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


public record PasswordResetInputEmailDTO(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email
){ }
