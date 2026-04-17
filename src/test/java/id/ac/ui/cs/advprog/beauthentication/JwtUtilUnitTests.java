package id.ac.ui.cs.advprog.beauthentication;

import id.ac.ui.cs.advprog.beauthentication.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilUnitTests {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "12345678901234567890123456789012");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 60000L);
    }

    @Test
    void shouldGenerateTokenAndExtractUsername() {
        String token = jwtUtil.generateToken("user@example.com");

        assertNotNull(token);
        assertEquals("user@example.com", jwtUtil.extractUsername(token));
    }

    @Test
    void shouldGenerateTokenWithRoleAndExtractRole() {
        String token = jwtUtil.generateToken("admin@example.com", "ADMIN");

        assertEquals("ADMIN", jwtUtil.extractRole(token));
    }

    @Test
    void shouldNotSetRoleClaimWhenRoleIsNullOrBlank() {
        String tokenWithNull = jwtUtil.generateToken("user@example.com", null);
        String tokenWithBlank = jwtUtil.generateToken("user@example.com", "   ");

        assertNull(jwtUtil.extractRole(tokenWithNull));
        assertNull(jwtUtil.extractRole(tokenWithBlank));
    }

    @Test
    void shouldExtractExpirationFromToken() {
        String token = jwtUtil.generateToken("user@example.com");

        Date expiration = jwtUtil.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date(System.currentTimeMillis() - 1000)));
    }

    @Test
    void shouldValidateValidToken() {
        String token = jwtUtil.generateToken("user@example.com", "TITIPER");

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void shouldRejectMalformedToken() {
        assertFalse(jwtUtil.validateToken("not-a-jwt-token"));
    }

    @Test
    void shouldRejectExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
        String expiredToken = jwtUtil.generateToken("user@example.com", "TITIPER");

        assertFalse(jwtUtil.validateToken(expiredToken));
    }
}
