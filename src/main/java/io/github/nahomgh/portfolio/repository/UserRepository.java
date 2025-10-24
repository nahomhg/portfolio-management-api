package io.github.nahomgh.portfolio.repository;

import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.exceptions.UserNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id) throws UserNotFoundException;

    Optional<User> findByVerificationCode(String verificationCode);

    Optional<User> findByUsername(String username);
}
