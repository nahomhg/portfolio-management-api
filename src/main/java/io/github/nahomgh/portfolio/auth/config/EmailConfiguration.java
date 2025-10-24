package io.github.nahomgh.portfolio.auth.config;

import io.jsonwebtoken.security.Password;
import org.apache.hc.core5.http.ssl.TLS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class EmailConfiguration {

    @Value("${spring.mail.username:}")
    private String emailUsername;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${spring.mail.host:}")
    private String host;

    @Value("${spring.mail.port:}")
    private int port;

    @Bean
    public JavaMailSender javaMailSender(){
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(emailUsername);
        mailSender.setPassword(password);

        Properties properties = mailSender.getJavaMailProperties();
        properties.put("mail.transport.protocol","smtp");
        properties.put("mail.smtp.auth","true");
        properties.put("mail.smtp.starttls.enable","true");
        properties.put("debug",true);

        return mailSender;
    }


}
