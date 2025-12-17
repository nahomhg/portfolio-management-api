package io.github.nahomgh.portfolio.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyUserDTO {

    @NotBlank(message = "Email is required.")
    @Email(message = "Email must be valid.")
    private String email;

    @NotBlank(message = "Verification Code CANNOT be blank.")
    @Pattern(regexp = "^[A-Z0-9]{8}$", message = "Verification code must be exactly 8 alphanumeric characters.")
    private String verificationCode;

}
