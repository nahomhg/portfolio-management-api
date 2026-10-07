package io.github.nahomgh.portfolio.auth.service;


import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class VerificationCodeGeneration {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateCode(){
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return code.toString();
    }
}
