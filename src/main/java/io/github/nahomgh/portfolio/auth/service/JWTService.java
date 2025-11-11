package io.github.nahomgh.portfolio.auth.service;

import io.github.nahomgh.portfolio.auth.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JWTService {

    @Value("${security.jwt.secret-key:}")
    private String secretKeyBase64;

    @Value("${security.jwt.expiration-time:3600000}")
    private long expirationMs;

    private static final Logger logger = LoggerFactory.getLogger(JWTService.class);

    private SecretKey signingKey;


    @PostConstruct
    public void initKey(){

        if((secretKeyBase64 == null) || (secretKeyBase64.isBlank())){
            try{
                KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
                signingKey = keyGenerator.generateKey();
                secretKeyBase64 = Base64.getEncoder().encodeToString(signingKey.getEncoded());
                logger.warn("No JWT secret configured. Generated a temporary key for this run, token will be invalid after restart.");
            }catch (GeneralSecurityException e){
                throw new IllegalStateException("Failed to initialise JWT signing key!",e);
            }
        }else{
            byte[] keyBytes = Decoders.BASE64.decode(secretKeyBase64);
            signingKey = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    public String generateToken(String userEmail){

        return Jwts.builder()
                .subject(userEmail)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    public String generateToken(Map<String, Object> claims, String userEmail){
        return Jwts.builder()
                .claims(claims)
                .subject(userEmail)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    public String extractUserEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver){
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String subject = extractUserEmail(token);
        if(isTokenExpired(token))
            return false;

        if(subject.equals(userDetails.getUsername()))
            return true;

        if(userDetails instanceof User user)
            return subject.equals(user.getEmail());

        return false;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token){
        return extractClaim(token, Claims::getExpiration);
    }
}
