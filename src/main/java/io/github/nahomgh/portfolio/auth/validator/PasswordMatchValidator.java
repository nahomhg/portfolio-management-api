package io.github.nahomgh.portfolio.auth.validator;

import io.github.nahomgh.portfolio.auth.dto.RegisterDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatchCheck, RegisterDTO> {

    @Override
    public boolean isValid(RegisterDTO registerDTO, ConstraintValidatorContext constraintValidatorContext) {
        return registerDTO.password()!=null && registerDTO.password().equals(registerDTO.confirmedPassword());
    }
}
