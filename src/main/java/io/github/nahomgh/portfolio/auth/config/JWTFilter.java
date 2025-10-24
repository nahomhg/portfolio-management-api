package io.github.nahomgh.portfolio.auth.config;

import io.github.nahomgh.portfolio.auth.domain.MyUserDetailService;
import io.github.nahomgh.portfolio.auth.service.JWTService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JWTFilter extends OncePerRequestFilter {

    private final JWTService service;

    private final MyUserDetailService userDetailService;

    public JWTFilter(JWTService service, MyUserDetailService userDetailService) {
        this.service = service;
        this.userDetailService = userDetailService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = null;
        String subject = null;
        String authHeader = request.getHeader("Authorization");
        if(authHeader!=null && authHeader.startsWith("Bearer ")){
            token = authHeader.substring(7);
            try{
                subject = service.extractUserEmail(token);
            }catch(JwtException e){
                logger.error("Unable to get user information!",e);
            }
        }

        if((subject!=null) && (SecurityContextHolder.getContext().getAuthentication() == null)) {
            UserDetails userDetails = userDetailService.loadUserByUsername(subject);

            if (service.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken
                        = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request,response);
    }
}
