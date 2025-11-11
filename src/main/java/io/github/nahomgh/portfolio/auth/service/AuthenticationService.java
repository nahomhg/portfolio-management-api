package io.github.nahomgh.portfolio.auth.service;

import io.github.nahomgh.portfolio.auth.dto.LoginRequestDTO;
import io.github.nahomgh.portfolio.auth.dto.VerifyUserDTO;
import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.dto.UserDTO;
import io.github.nahomgh.portfolio.exceptions.*;
import io.github.nahomgh.portfolio.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;

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

    public UserDTO signUp(User userInfo) {
        Optional<User> user = userRepository.findByEmail(userInfo.getEmail());
        if(user.isEmpty()){
            userInfo.setPassword(encoder.encode(userInfo.getPassword()));
            userInfo.setVerificationCode(generateVerificationCode());
            userInfo.setVerificationExpiration(Instant.now().plusSeconds(900));
            User saved_user = userRepository.save(userInfo);
            sendVerificationEmail(userInfo);
            logger.info("SUCCESS: User created");
            return new UserDTO(saved_user);
        }
        logger.error("ERROR: User already exists");
        throw new UserAlreadyExistsException("ERROR: User with email "+userInfo.getEmail()+" ALREADY exists");
    }

    public String authenticate(LoginRequestDTO loginRequest) {
        User user = findUser(loginRequest.email());
        if(!user.isEnabled()) {
            throw new RuntimeException("Account not verified, please verify your account");
        }
        Authentication authentication =
                authManager.authenticate(
                        new UsernamePasswordAuthenticationToken(loginRequest.email(),loginRequest.password()));
        if(authentication.isAuthenticated()) {
            return jwtService.generateToken(loginRequest.email());

        }
        throw new UserNotFoundException("Unable to verify user");
    }

    public void verifyUser(VerifyUserDTO verificationInput) {
        User user = findUser(verificationInput.getEmail());
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

    public void resendVerificationCode(String email){
        User user = findUser(email);
        if(user.isEnabled())
            throw new RuntimeException("Account already verified");
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationExpiration(Instant.now().plusSeconds(900));
        sendVerificationEmail(user);
        userRepository.save(user);
    }

    public void sendVerificationEmail(User user) {
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

        try{
            emailService.sendVerificationEmail(user.getEmail(),subject,htmlMsg);
        }catch(MessagingException e){
            logger.error("ERROR: Failed to send verification code to "+user.getEmail()+":\n"+e.getMessage());
        }
    }

    private String generateVerificationCode(){
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // Ensures 6 digits
        return String.valueOf(code);
    }

    private User getAuthenticatedUser(){
        return ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    private User findUser(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User with those details NOT found"));
    }
}
