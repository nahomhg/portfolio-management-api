package io.github.nahomgh.portfolio.auth.service;

import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.exceptions.UserNotFoundException;
import io.github.nahomgh.portfolio.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserQueryService {
    private final UserRepository userRepository;

    public UserQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUserByEmail(String email){
        return userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("Account NOT found"));
    }

    public User getUserByID(Long id){
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("Account NOT found"));
    }

}
