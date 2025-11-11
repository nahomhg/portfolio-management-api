package io.github.nahomgh.portfolio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.nahomgh.portfolio.auth.validator.PasswordMatchCheck;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


@PasswordMatchCheck
public record RegisterDTO (
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 30, message = "Username must be between 3-30 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, hyphens, and underscores")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8-128 characters")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String password,

        @NotBlank(message = "Password is required")
        String confirmedPassword
){ }
