package io.github.nahomgh.portfolio.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetVerificationDTO(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Verification Code CANNOT be blank.")
        @Pattern(regexp = "^[A-Z0-9]{8}$", message = "Verification code must be exactly 8 alphanumeric characters.")
        String verificationCode,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8-128 characters")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String password
)
{ }
