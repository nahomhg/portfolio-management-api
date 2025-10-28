package io.github.nahomgh.portfolio.service;

import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.auth.service.EmailService;
import io.github.nahomgh.portfolio.dto.UserDTO;
import io.github.nahomgh.portfolio.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public UserDTO getProfile(Long userId) {
        return new UserDTO(userRepository.findById(userId).get());
    }
    private User getAuthenticatedUser(){
        return ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }
}
