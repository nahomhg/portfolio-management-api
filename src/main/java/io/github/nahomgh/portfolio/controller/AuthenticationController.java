package io.github.nahomgh.portfolio.controller;

import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.auth.dto.AuthResponseDTO;
import io.github.nahomgh.portfolio.auth.dto.LoginRequestDTO;
import io.github.nahomgh.portfolio.auth.dto.ResendVerificationCodeDTO;
import io.github.nahomgh.portfolio.auth.dto.VerifyUserDTO;
import io.github.nahomgh.portfolio.auth.service.JWTService;
import io.github.nahomgh.portfolio.dto.UserDTO;
import io.github.nahomgh.portfolio.auth.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.signUp(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        String token = userService.authenticate(loginRequest);
        long expiresIn = jwtService.getExpirationMs();
        return ResponseEntity.ok(new AuthResponseDTO(token, expiresIn));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyUserDTO verifyUserDTO) {
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
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

}