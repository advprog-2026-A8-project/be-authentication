package id.ac.ui.cs.advprog.beauthentication.config;

import id.ac.ui.cs.advprog.beauthentication.model.AccountStatus;
import id.ac.ui.cs.advprog.beauthentication.model.UserProfile;
import id.ac.ui.cs.advprog.beauthentication.repository.UserProfileRepository;
import id.ac.ui.cs.advprog.beauthentication.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserProfileRepository userProfileRepository;

    private List<SimpleGrantedAuthority> toAuthorities(String role) {
        if (role == null || role.isBlank()) {
            return Collections.emptyList();
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return List.of(new SimpleGrantedAuthority("ROLE_" + normalized));
    }

    private boolean isVerifyEndpoint(HttpServletRequest request) {
        return "/api/auth/verify".equals(request.getRequestURI());
    }

    private Optional<UserProfile> findUserBySubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return Optional.empty();
        }

        String trimmed = subject.trim();
        Optional<UserProfile> byUsername = userProfileRepository.findByUsername(trimmed);
        if (byUsername.isPresent()) {
            return byUsername;
        }

        String normalizedEmail = trimmed.toLowerCase(Locale.ROOT);
        return userProfileRepository.findByEmail(normalizedEmail);
    }

    private boolean isBannedUser(String subject) {
        return findUserBySubject(subject)
                .map(UserProfile::getAccountStatus)
                .map(status -> AccountStatus.BANNED.name().equalsIgnoreCase(status))
                .orElse(false);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // Validasi token
            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);

                List<SimpleGrantedAuthority> authorities = toAuthorities(role);
                if (authorities.isEmpty()) {
                    chain.doFilter(request, response);
                    return;
                }

                if (!isVerifyEndpoint(request) && isBannedUser(username)) {
                    chain.doFilter(request, response);
                    return;
                }

                // Beri tahu Spring Security bahwa pengguna ini sudah sah (terautentikasi)
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        chain.doFilter(request, response);
    }
}