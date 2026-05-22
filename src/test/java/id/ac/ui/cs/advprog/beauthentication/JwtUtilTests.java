package id.ac.ui.cs.advprog.beauthentication;

import id.ac.ui.cs.advprog.beauthentication.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTests {

    private JwtUtil jwtUtil;

    private static final String TEST_SECRET = "TestSecretKeyUntukIntegrationTestYangCukupPanjang123!@#";
    private static final long TEST_EXPIRATION = 86400000L;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);
    }

    @Test
    void generateToken_withUsername_canExtractUsername() {
        String token = jwtUtil.generateToken("user@example.com");
        assertEquals("user@example.com", jwtUtil.extractUsername(token));
    }

    @Test
    void generateToken_withRole_canExtractRole() {
        String token = jwtUtil.generateToken("user@example.com", "TITIPER");
        assertEquals("TITIPER", jwtUtil.extractRole(token));
    }

    @Test
    void generateToken_withRole_canExtractExpiration() {
        String token = jwtUtil.generateToken("user@example.com", "TITIPER");
        Date exp = jwtUtil.extractExpiration(token);
        assertNotNull(exp);
        assertTrue(exp.after(new Date()));
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtil.generateToken("user@example.com");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
        String token = jwtUtil.generateToken("user@example.com");
        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken("user@example.com");
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertFalse(jwtUtil.validateToken(tampered));
    }

    @Test
    void validateToken_differentSecret_returnsFalse() {
        JwtUtil otherUtil = new JwtUtil();
        ReflectionTestUtils.setField(otherUtil, "secret", "OtherSecretKeyUntukJwtTestDenganPanjangYangCukup!@#");
        ReflectionTestUtils.setField(otherUtil, "expiration", TEST_EXPIRATION);

        String tokenFromOther = otherUtil.generateToken("user@example.com");
        assertFalse(jwtUtil.validateToken(tokenFromOther));
    }

    @Test
    void extractUsername_valid_returnsSubject() {
        String token = jwtUtil.generateToken("subject@test.com");
        assertEquals("subject@test.com", jwtUtil.extractUsername(token));
    }

    @Test
    void extractRole_noRoleClaim_returnsNull() {
        String token = jwtUtil.generateToken("user@example.com");
        assertNull(jwtUtil.extractRole(token));
    }

    @Test
    void generateToken_isNotExpiredImmediately() {
        String token = jwtUtil.generateToken("user@example.com");
        assertTrue(jwtUtil.extractExpiration(token).after(new Date()));
    }

    @Test
    void generateToken_withUserId_canExtractUserId() {
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        String token = jwtUtil.generateToken("user@example.com", "TITIPER", userId);
        assertEquals(userId, jwtUtil.extractUserId(token));
    }

    @Test
    void extractUserId_noUserIdClaim_returnsNull() {
        String token = jwtUtil.generateToken("user@example.com", "TITIPER");
        assertNull(jwtUtil.extractUserId(token));
    }

    @Test
    void validateToken_expiredToken_isNotCached() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
        String expiredToken = jwtUtil.generateToken("user@example.com");
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);

        assertFalse(jwtUtil.validateToken(expiredToken));
        assertNull(jwtUtil.getClaimsCache().getIfPresent(expiredToken));
        assertFalse(jwtUtil.validateToken(expiredToken));
    }

    @Test
    void validateToken_validToken_isCachedAfterFirstCall() {
        String token = jwtUtil.generateToken("user@example.com");

        assertNull(jwtUtil.getClaimsCache().getIfPresent(token));
        assertTrue(jwtUtil.validateToken(token));
        assertNotNull(jwtUtil.getClaimsCache().getIfPresent(token));
    }
}
