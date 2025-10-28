package io.github.nahomgh.portfolio.controller;

import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("api/v1/user")
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getUserDetails(Authentication authentication){
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(userService.getProfile(user.getId()));
    }
}
