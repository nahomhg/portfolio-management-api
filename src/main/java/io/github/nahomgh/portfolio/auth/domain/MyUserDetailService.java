package io.github.nahomgh.portfolio.auth.domain;
import io.github.nahomgh.portfolio.exceptions.UserNotFoundException;
import io.github.nahomgh.portfolio.repository.UserRepository;
import io.github.nahomgh.portfolio.auth.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MyUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(MyUserDetailService.class);

    public MyUserDetailService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByUsername(username);

        if(user.isEmpty())
            user = userRepository.findByEmail(username); // Could be an email

        return user.orElseThrow(()-> {
            logger.error("Cannot Find User");
            return new UserNotFoundException("User NOT found");
        });
    }
}
