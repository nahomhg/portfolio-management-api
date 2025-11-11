package io.github.nahomgh.portfolio.auth.validator;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordMatchValidator.class)
@Documented
public @interface PasswordMatchCheck {
    String message() default "Passwords do NOT match!";
    Class<? >[] groups() default{};
    Class<? extends Payload>[] payload() default {};
}
