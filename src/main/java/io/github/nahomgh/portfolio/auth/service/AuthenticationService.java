package io.github.nahomgh.portfolio.auth.service;

import io.github.nahomgh.portfolio.auth.dto.*;
import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.exceptions.*;
import io.github.nahomgh.portfolio.repository.UserRepository;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.Optional;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final AuthenticationManager authManager;
    private final VerificationCodeService verificationCodeService;
    private final ApplicationEventPublisher events;
    private final UserQueryService userQueryService;
    private final JWTService jwtService;

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);


    public AuthenticationService(UserRepository userRepository, AuthenticationManager authManager, VerificationCodeService verificationCodeService, ApplicationEventPublisher events, UserQueryService userQueryService,
                                 JWTService jwtService) {
        this.userRepository = userRepository;
        this.authManager = authManager;
        this.verificationCodeService = verificationCodeService;
        this.events = events;
        this.userQueryService = userQueryService;
        this.jwtService = jwtService;

    }

    public UserDTO getUser() {
        return new UserDTO(userQueryService.getUserByID(getAuthenticatedUser().getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public UserDTO signUp(RegisterDTO userSignUpDetails){
        if(userRepository.findByEmail(userSignUpDetails.email()).isPresent()){
            throw new UserAlreadyExistsException("Email Already Taken");
        }

        User registeredUser = new User();
        registeredUser.setEmail(userSignUpDetails.email());
        registeredUser.setUsername(userSignUpDetails.username());
        registeredUser.setPassword(encoder.encode(userSignUpDetails.password()));
        registeredUser.setEnabled(false);
        userRepository.save(registeredUser);

        events.publishEvent(new UserRegisteredEvent(registeredUser.getEmail()));

        return new UserDTO(registeredUser);
    }

    @TransactionalEventListener(phase= TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleRegisterVerificationEmail(UserRegisteredEvent event){
        try{
            userRepository.findByEmail(event.email()).ifPresent(verificationCodeService::generateVerificationCode);
        } catch (EmailDeliveryException e) {
            logger.error("Verification email failed for {}", event.email(), e);
        }

    }

    public String authenticate(LoginRequestDTO loginRequest) {
        String userEmail = StringEscapeUtils.escapeHtml4(loginRequest.email());
        Optional<User> user = findUser(userEmail);

        if (user.isEmpty() || !encoder.matches(loginRequest.password(), user.get().getPassword())) {
            throw new AuthenticationException("Invalid email or password.");
        }else if (!user.get().isEnabled()) {
            throw new AuthenticationException("Unverified user, please verify account");
        }

        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(userEmail, loginRequest.password()));
        return jwtService.generateToken(userEmail);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetVerificationCode(String email){
        try {
            User user = userQueryService.getUserByEmail(email);
            verificationCodeService.generateVerificationCode(user);
        }catch (UserNotFoundException e){
            logger.info("User NOT found");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void verifyAccount(String email, String code) {
        String inputEmail = StringEscapeUtils.escapeHtml4(email);
        String inputCode = StringEscapeUtils.escapeHtml4(code);
        User user = userQueryService.getUserByEmail(inputEmail);

        verificationCodeService.verifyCode(user, inputCode);
        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationExpiration(null);
        userRepository.save(user);
    }

    public void resetPassword(PasswordResetVerificationDTO passwordResetVerificationDTO) {
        User user = findUser(passwordResetVerificationDTO.email()).orElseThrow(() -> new UserNotFoundException(""));
        verifyAccount(passwordResetVerificationDTO.email(), passwordResetVerificationDTO.verificationCode());
        user.setPassword(encoder.encode(passwordResetVerificationDTO.password()));
        userRepository.save(user);
    }


    private User getAuthenticatedUser() {
        return ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    private Optional<User> findUser(String email) {
        return userRepository.findByEmail(email);
    }

}
