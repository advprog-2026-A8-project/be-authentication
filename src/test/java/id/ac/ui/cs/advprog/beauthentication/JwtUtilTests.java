package id.ac.ui.cs.advprog.beauthentication;

import id.ac.ui.cs.advprog.beauthentication.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

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
}
