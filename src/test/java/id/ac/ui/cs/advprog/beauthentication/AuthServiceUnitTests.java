package id.ac.ui.cs.advprog.beauthentication;

import id.ac.ui.cs.advprog.beauthentication.dto.LoginRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.RegisterRequest;
import id.ac.ui.cs.advprog.beauthentication.model.KycStatus;
import id.ac.ui.cs.advprog.beauthentication.model.UserProfile;
import id.ac.ui.cs.advprog.beauthentication.model.UserRole;
import id.ac.ui.cs.advprog.beauthentication.repository.UserProfileRepository;
import id.ac.ui.cs.advprog.beauthentication.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTests {

    @Mock
    private UserProfileRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRejectNullRegisterRequest() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(null));
        assertEquals("Request tidak boleh kosong!", ex.getMessage());
    }

    @Test
    void shouldRejectRegisterWhenEmailOrPasswordBlank() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(" ");
        request.setPassword("password123");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        assertEquals("Email dan password wajib diisi!", ex.getMessage());
    }

    @Test
    void shouldRejectRegisterWhenPasswordTooShort() {
        RegisterRequest request = registerRequest(null, "user@example.com", "short");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        assertEquals("Password minimal 8 karakter!", ex.getMessage());
    }

    @Test
    void shouldRejectRegisterWhenEmailFormatInvalid() {
        RegisterRequest request = registerRequest(null, "invalid-email", "password123");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        assertEquals("Format email tidak valid!", ex.getMessage());
    }

    @Test
    void shouldRejectRegisterWhenUsernameAlreadyExists() {
        RegisterRequest request = registerRequest("existing_user", "user@example.com", "password123");
        when(repository.findByUsername("existing_user")).thenReturn(Optional.of(new UserProfile()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        assertEquals("Username atau Email sudah terdaftar!", ex.getMessage());
    }

    @Test
    void shouldRejectRegisterWhenEmailAlreadyExists() {
        RegisterRequest request = registerRequest("new_user", "user@example.com", "password123");
        when(repository.findByUsername("new_user")).thenReturn(Optional.empty());
        when(repository.findByEmail("user@example.com")).thenReturn(Optional.of(new UserProfile()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        assertEquals("Username atau Email sudah terdaftar!", ex.getMessage());
    }

    @Test
    void shouldAutoGenerateUsernameWhenNotProvided() {
        RegisterRequest request = registerRequest(null, "my.user@example.com", "password123");

        when(repository.findByUsername("my_user")).thenReturn(Optional.empty());
        when(repository.findByEmail("my.user@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile saved = authService.register(request);

        assertEquals("my_user", saved.getUsername());
        assertEquals("my.user@example.com", saved.getEmail());
        assertEquals("encoded-pass", saved.getPassword());
        assertEquals(UserRole.TITIPER.name(), saved.getRole());
        assertEquals(KycStatus.PENDING.name(), saved.getKycStatus());
    }

    @Test
    void shouldGenerateUsernameWithSuffixWhenCollisionOccurs() {
        RegisterRequest request = registerRequest(null, "alice@example.com", "password123");

        when(repository.findByUsername(anyString())).thenAnswer(invocation -> {
            String candidate = invocation.getArgument(0);
            if ("alice".equals(candidate) || "alice_1".equals(candidate)) {
                return Optional.of(new UserProfile());
            }
            return Optional.empty();
        });
        when(repository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile saved = authService.register(request);

        assertEquals("alice_2", saved.getUsername());
    }

    @Test
    void shouldNormalizeEmailOnRegister() {
        RegisterRequest request = registerRequest("custom_user", "  USER@Example.COM  ", "password123");

        when(repository.findByUsername("custom_user")).thenReturn(Optional.empty());
        when(repository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile saved = authService.register(request);

        assertEquals("user@example.com", saved.getEmail());
    }

    @Test
    void shouldRejectNullLoginRequest() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.login(null));
        assertEquals("Request tidak boleh kosong!", ex.getMessage());
    }

    @Test
    void shouldRejectLoginWhenEmailOrPasswordBlank() {
        LoginRequest request = loginRequest(" ", "password123");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        assertEquals("Email dan password wajib diisi!", ex.getMessage());
    }

    @Test
    void shouldRejectLoginWhenEmailFormatInvalid() {
        LoginRequest request = loginRequest("invalid-email", "password123");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        assertEquals("Format email tidak valid!", ex.getMessage());
    }

    @Test
    void shouldRejectLoginWhenEmailNotFound() {
        LoginRequest request = loginRequest("user@example.com", "password123");
        when(repository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        assertEquals("Email tidak ditemukan!", ex.getMessage());
    }

    @Test
    void shouldRejectLoginWhenPasswordDoesNotMatch() {
        LoginRequest request = loginRequest("user@example.com", "wrong-password");
        UserProfile user = new UserProfile();
        user.setEmail("user@example.com");
        user.setPassword("encoded-pass");

        when(repository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-pass")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        assertEquals("Password salah!", ex.getMessage());
    }

    @Test
    void shouldLoginSuccessfullyAndNormalizeEmail() {
        LoginRequest request = loginRequest("  USER@example.com ", "password123");
        UserProfile user = new UserProfile();
        user.setEmail("user@example.com");
        user.setPassword("encoded-pass");

        when(repository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-pass")).thenReturn(true);

        UserProfile result = authService.login(request);

        assertSame(user, result);
        verify(repository).findByEmail("user@example.com");
        verify(passwordEncoder).matches("password123", "encoded-pass");
    }

    private RegisterRequest registerRequest(String username, String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
}
