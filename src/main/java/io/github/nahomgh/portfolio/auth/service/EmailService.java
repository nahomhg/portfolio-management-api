package io.github.nahomgh.portfolio.auth.service;

import io.github.nahomgh.portfolio.exceptions.EmailDeliveryException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {


    private final JavaMailSender emailSender;
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void sendVerificationEmail(String recipient, String subject, String contents) {
        try {
            logger.info("starting class {}",this.getClass().getCanonicalName());
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            logger.info("mid method {}",this.getClass().getCanonicalName());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(contents, true);
            logger.info("sending email: {}",this.getClass().getCanonicalName());
            emailSender.send(message);
            logger.info("ending method send verification of {}",this.getClass().getCanonicalName());
        } catch(MessagingException msg){
            logger.error("Email Delivery Failed:\n{}",msg.getMessage());
            throw new EmailDeliveryException("Unable to send email. Please try again later",msg);
        }
    }
}
