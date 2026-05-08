package com.aiu.proctoring.infrastructure.security;

import com.aiu.proctoring.domain.value.UserId;
import com.aiu.proctoring.infrastructure.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that extracts user ID from JWT and sets it as a request attribute
 * for use in @RequestAttribute("userId") controller method arguments.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserAttributeFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                jwtService.validateToken(token);
                String username = jwtService.extractUsername(token);
                userRepository.findByUsername(username).ifPresent(user -> {
                    request.setAttribute("userId", user.getId().getValue());
                });
            } catch (JwtException e) {
                log.debug("JWT validation failed: {}", e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }
}
