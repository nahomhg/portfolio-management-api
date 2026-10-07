package io.github.nahomgh.portfolio.auth.service;

import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.exceptions.InvalidVerificationCodeException;
import io.github.nahomgh.portfolio.exceptions.VerificationExpirationException;
import io.github.nahomgh.portfolio.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private VerificationCodeGeneration verificationCodeGeneration;

    private VerificationCodeService verificationCodeService;

    private User testUser;
    private String rawCode;
    private String hashCode;


    @BeforeEach
    void setUp(){
        verificationCodeService = new VerificationCodeService(userRepository, emailService, verificationCodeGeneration);
        testUser = new User("nahom_gh@outlook.com", "nahomg", "pass12345");
        testUser.setId(1L);
        rawCode = "ABCD1234";
        hashCode = "1635c8525afbae58c37bede3c9440844e9143727cc7c160bed665ec378d8a262";
        testUser.setVerificationCode(hashCode);
        testUser.setVerificationExpiration(Instant.now().plusSeconds(600));
        ReflectionTestUtils.setField(verificationCodeService, "verificationCodeTemplate", "Hello %s ,  Your code is: %s");
    }

    @Test
    void correctCodeAttempt(){
        assertDoesNotThrow(() -> verificationCodeService.verifyCode(testUser, rawCode));
    }

    @Test
    void incorrectCodeAttemptThrowsInvalidVerificationCodeException() {
        assertThrows(InvalidVerificationCodeException.class, () -> {
            verificationCodeService.verifyCode(testUser, "ABSA1234");
        });
    }

    @Test
    void oldCodeAttemptThrowsVerificationExpirationException() {
        testUser.setVerificationExpiration(Instant.now().minusSeconds(600));

        assertThrows(VerificationExpirationException.class, () -> {
            verificationCodeService.verifyCode(testUser, rawCode);
        });
    }

    @Test
    void generateVerificationCodeSetsHashCodeAndSendsEmail(){
        String code = "MYTEST1234";
        String expectedHashCode = "05c34d5f052ebc79f750b4071472448cc2740ee5475ffa50d64b2b5e0ca72ccf";
        Mockito.when(verificationCodeGeneration.generateCode()).thenReturn(code);

        verificationCodeService.generateVerificationCode(testUser);
        assertEquals(expectedHashCode,testUser.getVerificationCode());

        assertNotNull(testUser.getVerificationExpiration());

        Mockito.verify(userRepository).save(testUser);
        Mockito.verify(emailService).sendVerificationEmail(
                Mockito.eq(testUser.getEmail()), Mockito.anyString(), Mockito.anyString()
        );

    }
}