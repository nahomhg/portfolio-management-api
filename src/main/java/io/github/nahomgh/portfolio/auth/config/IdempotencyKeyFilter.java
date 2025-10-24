package io.github.nahomgh.portfolio.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.regex.Pattern;

public class IdempotencyKeyFilter extends OncePerRequestFilter {

    private static final Pattern ALLOWED = Pattern.compile("^[A-Za-z0-9._-]{1,75}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Only guard POST /transactions (adjust to your prefix, e.g. /api/v1/transactions)
        String path = request.getRequestURI();
        boolean isTransactionsPost = "POST".equalsIgnoreCase(request.getMethod())
                && path.startsWith("/api/v1/transactions");

        if (!isTransactionsPost) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getHeader("IDEMPOTENCY_KEY");
        System.out.println("Key is : "+key);
        if (key == null || key.isBlank() || !ALLOWED.matcher(key).matches()) {
            writeBadRequest(response, "Invalid or missing Idempotency-Key");
            return; // short-circuit
        }

        // Normalize & propagate for downstream usage/logging
        String normalized = key.trim();
        request.setAttribute("IDEMPOTENCY_KEY", normalized);
        MDC.put("idempotencyKey", normalized);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("idempotencyKey");
        }
    }

    private void writeBadRequest(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.getWriter().write("""
      {"error":"ValidationError","message":"%s"}
      """.formatted(message));
        response.getWriter().flush();
    }
}

