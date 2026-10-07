package io.github.nahomgh.portfolio.auth.controller;

import io.github.nahomgh.portfolio.auth.dto.*;
import io.github.nahomgh.portfolio.auth.service.JWTService;
import io.github.nahomgh.portfolio.auth.service.AuthenticationService;
import io.github.nahomgh.portfolio.auth.service.VerificationCodeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/v1/auth")
@RestControllerAdvice
public class AuthenticationController {

    private final AuthenticationService userService;
    private final JWTService jwtService;
    private final VerificationCodeService verificationCodeService;

    public AuthenticationController(AuthenticationService userService, JWTService jwtService, VerificationCodeService verificationCodeService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.verificationCodeService = verificationCodeService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> createUser(@Valid @RequestBody RegisterDTO registeredUser) {
          userService.signUp(registeredUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message","Please check provided email address for verification code."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        String token = userService.authenticate(loginRequest);
        long expiresIn = jwtService.getExpirationMs();
        return ResponseEntity.ok(new AuthResponseDTO(token, expiresIn));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@Valid @RequestBody VerifyUserDTO verifyUserDTO) {
        userService.verifyAccount(verifyUserDTO.getEmail(), verifyUserDTO.getVerificationCode());
        return ResponseEntity.ok("Account verified!");
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resendVerificationEmail(@Valid @RequestBody ResendVerificationCodeDTO userDetails) {
        userService.resetVerificationCode(userDetails.email());
        return ResponseEntity.ok("Verification Code will be sent if account with provided exists.");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody PasswordResetInputEmailDTO passwordResetInputEmailDTO){
        userService.resetVerificationCode(passwordResetInputEmailDTO.email());
        return ResponseEntity.ok("Sending verification code to email if user with provided email exists");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetVerificationDTO passwordReset){
        userService.resetPassword(passwordReset);
        return ResponseEntity.ok("Password rest complete!");
    }
}