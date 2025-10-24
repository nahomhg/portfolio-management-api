package io.github.nahomgh.portfolio.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyUserDTO {

    private String email;
    private String verificationCode;

}
