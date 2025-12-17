package io.github.nahomgh.portfolio.service;

import io.github.nahomgh.portfolio.auth.service.EmailService;
import io.github.nahomgh.portfolio.auth.dto.UserDTO;
import io.github.nahomgh.portfolio.exceptions.UserNotFoundException;
import io.github.nahomgh.portfolio.repository.UserRepository;
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
        return new UserDTO(userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " NOT Found. Transaction NOT processed.")));
    }

}
