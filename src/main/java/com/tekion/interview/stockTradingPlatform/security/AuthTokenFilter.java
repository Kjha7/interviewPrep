package com.tekion.interview.stockTradingPlatform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Boilerplate auth: pulls "Authorization: Bearer <jwt>" off the request, validates it,
 * and stashes the resolved userId as a request attribute for controllers to read.
 * Public endpoints (/auth/**) are excluded in SecurityConfig / WebConfig path matching -
 * wire this into a proper Spring Security filter chain for anything beyond a prototype.
 */
@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    public static final String AUTHENTICATED_USER_ID_ATTR = "authenticatedUserId";

    private final JwtTokenProvider jwtTokenProvider;

    public AuthTokenFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            UUID userId = jwtTokenProvider.validateAndGetUserId(token); // throws -> handled by GlobalExceptionHandler
            request.setAttribute(AUTHENTICATED_USER_ID_ATTR, userId);
        }

        filterChain.doFilter(request, response);
    }
}
