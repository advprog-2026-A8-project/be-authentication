package id.ac.ui.cs.advprog.beauthentication.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;

    @Value("${rate.limit.login:5}")
    private int loginLimit;

    @Value("${rate.limit.register:3}")
    private int registerLimit;

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";

    private final Map<String, Deque<Long>> requestLogs = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (!"POST".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        int limit;
        if (LOGIN_PATH.equals(path)) {
            limit = loginLimit;
        } else if (REGISTER_PATH.equals(path)) {
            limit = registerLimit;
        } else {
            chain.doFilter(request, response);
            return;
        }

        String ip = resolveClientIp(request);
        String key = ip + "::" + path;

        if (isRateLimitExceeded(key, limit)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Terlalu banyak permintaan. Coba lagi nanti.\",\"data\":null}");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isRateLimitExceeded(String key, int limit) {
        Deque<Long> timestamps = requestLogs.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            long now = System.currentTimeMillis();
            long windowStart = now - WINDOW_MS;

            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= limit) {
                return true;
            }

            timestamps.addLast(now);
            return false;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
