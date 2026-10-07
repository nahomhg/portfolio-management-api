package io.github.nahomgh.portfolio.auth.service;

import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.exceptions.InvalidVerificationCodeException;
import io.github.nahomgh.portfolio.exceptions.UnavailableAlgorithmException;
import io.github.nahomgh.portfolio.exceptions.VerificationExpirationException;
import io.github.nahomgh.portfolio.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class VerificationCodeService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final VerificationCodeGeneration codeGeneration;

    @Getter
    private String verificationCodeTemplate;

    @Value("${auth.email.template-path:}")
    private String verificationEmailTemplate;

    private static final Logger logger = LoggerFactory.getLogger(VerificationCodeService.class);

    public VerificationCodeService(UserRepository userRepository, EmailService emailService, VerificationCodeGeneration codeGeneration){
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.codeGeneration = codeGeneration;
    }

    @PostConstruct
    public void initTemplate(){
        try{
            ClassPathResource classPathResource = new ClassPathResource(this.verificationEmailTemplate);
            this.verificationCodeTemplate = StreamUtils.copyToString(classPathResource.getInputStream(), StandardCharsets.UTF_8);
            logger.info("SUCCESS: Template has been found and completed loading from {}. Ready for use.", this.verificationEmailTemplate);
        }catch(IOException e){
            logger.error("Problem Connecting with HTML resource: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to load email verification code template", e);
        }
    }

    public void generateVerificationCode(User user) {
        String code = codeGeneration.generateCode();
        String hashedGeneratedCode = hashVerificationCode(code);
        user.setVerificationCode(hashedGeneratedCode);
        user.setVerificationExpiration(Instant.now().plusSeconds(600));
        userRepository.save(user);

        sendVerificationEmail(user, code);
    }

    private void sendVerificationEmail(User user, String code) {

        String subject = "Account Verification";
        String htmlMsg = getVerificationCodeTemplate().formatted(user.getUsername(), code);

        emailService.sendVerificationEmail(user.getEmail(), subject, htmlMsg);
    }

    public void verifyCode(User user, String inputCode){
        if (user.getVerificationExpiration().isBefore(Instant.now())) {
            throw new VerificationExpirationException("Verification Code Expired");
        }
        String hashedInputCode = hashVerificationCode(inputCode);
        if(!user.getVerificationCode().equals(hashedInputCode))
            throw new InvalidVerificationCodeException("ERROR: Invalid verification code. Unable to verify user");
    }

    private String hashVerificationCode(String code){
        try {
            MessageDigest msgDigest = MessageDigest.getInstance("SHA-256");
            var hashedInput = msgDigest.digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashedInput);
        }
        catch(NoSuchAlgorithmException e){
            throw new UnavailableAlgorithmException();
        }
    }

}
