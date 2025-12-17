package io.github.nahomgh.portfolio.auth.service;

import io.github.nahomgh.portfolio.auth.dto.LoginRequestDTO;
import io.github.nahomgh.portfolio.auth.dto.VerifyUserDTO;
import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.auth.dto.RegisterDTO;
import io.github.nahomgh.portfolio.auth.dto.UserDTO;
import io.github.nahomgh.portfolio.exceptions.*;
import io.github.nahomgh.portfolio.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import lombok.Getter;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final AuthenticationManager authManager;

    private final JWTService jwtService;

    @Getter
    private String verificationCodeTemplate;

    @Value("${auth.email.template-path:}")
    private String verificationEmailTemplate;

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    private final EmailService emailService;

    public AuthenticationService(UserRepository userRepository, AuthenticationManager authManager,
                                 JWTService jwtService, EmailService emailService) {
        this.userRepository = userRepository;
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.emailService = emailService;

    }

    @PostConstruct
    public void initTemplate(){
        try{ //this.verificationEmailTemplate)
            ClassPathResource classPathResource = new ClassPathResource(this.verificationEmailTemplate);
            this.verificationCodeTemplate = StreamUtils.copyToString(classPathResource.getInputStream(), StandardCharsets.UTF_8);
            logger.info("SUCCESS: Template has been found and completed loading from {}. Ready for use.",this.verificationEmailTemplate);
        }catch(IOException e){
            logger.error("Problem Connecting with HTML resource");
            throw new IllegalStateException("Failed to load email verification code template");
        }
    }

    public UserDTO getUser() {
        User authenticatedUser = getAuthenticatedUser();
        Optional<User> user = userRepository.findById(authenticatedUser.getId());
        if (user.isPresent())
            return new UserDTO(user.get());
        throw new ResourceNotFoundException("ERROR: Account NOT associated with current user");
    }

    @Transactional(rollbackFor = Exception.class)
    public UserDTO signUp(RegisterDTO userSignUpDetails) {
        Optional<User> user = userRepository.findByEmail(userSignUpDetails.email());
        if (user.isPresent()) {
            logger.error("Registration attempt for existing user");
            if (!user.get().isEnabled()) {
                try {
                    resendVerificationCode(user.get().getEmail());
                } catch (MessagingException exception) {
                    logger.error("Failed to re-send verification email to user");
                    logger.error("Exception: {}", exception.getMessage());
                }
                logger.info("Verification code sent via email to unverified user");
                return new UserDTO(user.get());
            }
            logger.info("Attempt to register already verified user");
            return new UserDTO(user.get());
        }
        User registeredUser = new User();
        registeredUser.setEmail(userSignUpDetails.email());
        registeredUser.setUsername(userSignUpDetails.username());
        registeredUser.setPassword(encoder.encode(userSignUpDetails.password()));
        registeredUser.setVerificationCode(generateVerificationCode());
        registeredUser.setVerificationExpiration(Instant.now().plusSeconds(900));
        registeredUser.setEnabled(false);

        userRepository.save(registeredUser);
        try {
            sendVerificationEmail(registeredUser);
        }catch(MessagingException me){
            logger.error("ERROR: Messaging Exception hit : {}",me.getMessage());
        }
        catch (Exception e) {
            logger.error("ERROR: Failed to send verification for email {}, rolling back sign up transaction.\n{}\n{}",
                    userSignUpDetails.email(), e.getMessage(), e.getStackTrace());
            throw new EmailDeliveryException("Unable to send email and complete email verification. Please try again.");
        }
        logger.info("SUCCESS: User created");
        return new UserDTO(registeredUser);
    }

    public String authenticate(LoginRequestDTO loginRequest) {
        String userEmail = StringEscapeUtils.escapeHtml4(loginRequest.email());
        User user = findUser(userEmail);

        if (user == null || !encoder.matches(loginRequest.password(), user.getPassword())) {
            throw new AuthenticationException("Invalid email or password.");
        }

        if (!user.isEnabled()) {
            throw new AuthenticationException("Invalid email or password.");
        }

        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(userEmail, loginRequest.password()));
        return jwtService.generateToken(userEmail);
    }

    public void verifyUser(VerifyUserDTO verificationInput) {
        String userEmail = StringEscapeUtils.escapeHtml4(verificationInput.getEmail());
        User user = findUser(userEmail);
        if (user.getVerificationExpiration().isBefore(Instant.now())) {
            throw new VerificationExpirationException("Verification Code Expired");
        }
        if (user.getVerificationCode().equals(verificationInput.getVerificationCode())) {
            user.setEnabled(true);
            user.setVerificationCode(null);
            user.setVerificationExpiration(null);
            userRepository.save(user);
        } else {
            throw new InvalidVerificationCodeException("ERROR: Invalid verification code. Unable to verify user");
        }
    }

    public void resendVerificationCode(String email) throws MessagingException {
        User user = findUser(email);
        if (user.isEnabled())
            throw new RuntimeException("Account already verified");
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationExpiration(Instant.now().plusSeconds(900));
        sendVerificationEmail(user);
        userRepository.save(user);
    }

    public void sendVerificationEmail(User user) throws MessagingException {

        String subject = "Account Verification";
        String verificationCode = user.getVerificationCode();
        String htmlMsg = getVerificationCodeTemplate().formatted(user.getUsername(), verificationCode);

        emailService.sendVerificationEmail(user.getEmail(), subject, htmlMsg);

    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    private User getAuthenticatedUser() {
        return ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User with those details NOT found"));
    }



}
