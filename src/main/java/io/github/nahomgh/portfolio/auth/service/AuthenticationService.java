package io.github.nahomgh.portfolio.auth.service;

import io.github.nahomgh.portfolio.auth.dto.LoginRequestDTO;
import io.github.nahomgh.portfolio.auth.dto.VerifyUserDTO;
import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.auth.dto.RegisterDTO;
import io.github.nahomgh.portfolio.auth.dto.UserDTO;
import io.github.nahomgh.portfolio.exceptions.*;
import io.github.nahomgh.portfolio.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

@Service
public class AuthenticationService {

    private UserRepository userRepository;
    private AuthenticationManager authManager;

    private JWTService jwtService;

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    private final EmailService emailService;

    public AuthenticationService(UserRepository userRepository, AuthenticationManager authManager, JWTService jwtService, EmailService emailService) {
        this.userRepository = userRepository;
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    public UserDTO getUser(){
        User authenticatedUser = getAuthenticatedUser();
        Optional<User> user = userRepository.findById(authenticatedUser.getId());
        if(user.isPresent())
            return new UserDTO(user.get());
        throw new ResourceNotFoundException("ERROR: Account NOT associated with current user");
    }

    @Transactional(rollbackFor = Exception.class)
    public UserDTO signUp(RegisterDTO userSignUpDetails) {
        Optional<User> user = userRepository.findByEmail(userSignUpDetails.email());
        if(user.isPresent()) {
            logger.error("Registration attempt for existing user");
            if(!user.get().isEnabled()) {
                try {
                    resendVerificationCode(user.get().getEmail());
                }catch(MessagingException exception){
                    logger.error("Failed to re-send verification email to user");
                    logger.error("Exception: "+ exception.getMessage());
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
        try{
            sendVerificationEmail(registeredUser);
        }catch(Exception e){
            logger.error("ERROR: Failed to send verification for email "+userSignUpDetails.email()+", rolling back sign up transaction.\n"+e.getMessage()+"\n"+e.getStackTrace());
            throw new EmailDeliveryException("Unable to send email and complete email verification. Please try again.");
        }
        logger.info("SUCCESS: User created");
        return new UserDTO(registeredUser);
    }

    public String authenticate(LoginRequestDTO loginRequest) {
        String userEmail = StringEscapeUtils.escapeHtml4(loginRequest.email());
        String userPw = StringEscapeUtils.escapeHtml4(loginRequest.password());
        User user = findUser(userEmail);
        if(!user.isEnabled()) {
            throw new AuthenticationException("Invalid email or password.");
        }
        Authentication authentication =
                authManager.authenticate(
                        new UsernamePasswordAuthenticationToken(userEmail,userPw));
        if(authentication.isAuthenticated()) {
            return jwtService.generateToken(userEmail);

        }
        throw new AuthenticationException("Invalid email or password.");
    }

    public void verifyUser(VerifyUserDTO verificationInput) {
        String userEmail = StringEscapeUtils.escapeHtml4(verificationInput.getEmail());
        User user = findUser(userEmail);
        if (user.getVerificationExpiration().isBefore(Instant.now())) {
            throw new VerificationExpirationException("Verification Code Expired");
        }
        if(user.getVerificationCode().equals(verificationInput.getVerificationCode())){
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationExpiration(null);
                userRepository.save(user);
            }
        else{
            throw new InvalidVerificationCodeException("ERROR: Invalid verification code. Unable to verify user");
        }
    }

    public void resendVerificationCode(String email) throws MessagingException{
        User user = findUser(email);
        if(user.isEnabled())
            throw new RuntimeException("Account already verified");
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationExpiration(Instant.now().plusSeconds(900));
        sendVerificationEmail(user);
        userRepository.save(user);
    }

    public void sendVerificationEmail(User user) throws MessagingException {
        String subject = "Account Verification";
        String verificationCode = user.getVerificationCode();
        String htmlMsg = """
          <!DOCTYPE html>
          <html>
          <head>
              <style>
                  body { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; }
                  .container { background: #f9f9f9; padding: 30px; border-radius: 8px; }
                  .code { font-size: 24px; font-weight: bold; color: #2563eb; text-align: center;
                          padding: 20px; background: white; border-radius: 6px; margin: 20px 0; }
              </style>
          </head>
          <body>
              <div class="container">
                  <h2>Verify Your Account</h2>
                  <p>Hi %s,</p>
                  <p>Use this code to verify your email:</p>
                  <div class="code">%s</div>
                  <p>This code expires in 15 minutes.</p>
              </div>
          </body>
          </html>
            """.formatted(user.getUsername(), verificationCode);

        emailService.sendVerificationEmail(user.getEmail(),subject,htmlMsg);
    }

    private String generateVerificationCode(){
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for(int i = 0; i < 8; i++){
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    private User getAuthenticatedUser(){
        return ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    private User findUser(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User with those details NOT found"));
    }
}
