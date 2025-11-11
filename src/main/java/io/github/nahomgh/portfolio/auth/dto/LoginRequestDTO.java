package io.github.nahomgh.portfolio.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(@Email String email,
                              @NotBlank
                              @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
                              String password) {
}
