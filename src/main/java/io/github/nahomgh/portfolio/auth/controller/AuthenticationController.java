package io.github.nahomgh.portfolio.auth.controller;

import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.auth.dto.AuthResponseDTO;
import io.github.nahomgh.portfolio.auth.dto.LoginRequestDTO;
import io.github.nahomgh.portfolio.auth.dto.ResendVerificationCodeDTO;
import io.github.nahomgh.portfolio.auth.dto.VerifyUserDTO;
import io.github.nahomgh.portfolio.auth.service.JWTService;
import io.github.nahomgh.portfolio.auth.dto.RegisterDTO;
import io.github.nahomgh.portfolio.auth.dto.UserDTO;
import io.github.nahomgh.portfolio.auth.service.AuthenticationService;
import io.github.nahomgh.portfolio.exceptions.EmailDeliveryException;
import io.github.nahomgh.portfolio.exceptions.ErrorResponse;
import io.github.nahomgh.portfolio.service.UserService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService userService;
    private final JWTService jwtService;

    public AuthenticationController(AuthenticationService userService, JWTService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> createUser(@Valid @RequestBody RegisterDTO registeredUser) {
        try {
            userService.signUp(registeredUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message","Please check provided email address for verification code."));
        }catch(EmailDeliveryException e){
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        String token = userService.authenticate(loginRequest);
        long expiresIn = jwtService.getExpirationMs();
        return ResponseEntity.ok(new AuthResponseDTO(token, expiresIn));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@Valid @RequestBody VerifyUserDTO verifyUserDTO) {
        try {
            userService.verifyUser(verifyUserDTO);
            return ResponseEntity.ok("Account verified!");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resendVerificationEmail(@RequestBody ResendVerificationCodeDTO userEmail) {
        try {
            userService.resendVerificationCode(userEmail.userEmail());
            return ResponseEntity.ok("Verification Code Sent to user");
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

}