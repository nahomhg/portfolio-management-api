package io.github.nahomgh.portfolio.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String password) {
}
