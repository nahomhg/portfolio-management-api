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

    private UserRepository repo;
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    public MyUserDetailService(UserRepository repo){
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = repo.findByUsername(username);

        if(user.isEmpty())
            user = repo.findByEmail(username); // Could be an email

        return user.orElseThrow(()->
                new UserNotFoundException("User NOT found"));
    }
}
