package id.ac.ui.cs.advprog.beauthentication;

import id.ac.ui.cs.advprog.beauthentication.dto.LoginRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.RegisterRequest;
import id.ac.ui.cs.advprog.beauthentication.model.AccountStatus;
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

    // ===================== HELPER =====================

    private RegisterRequest validRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("valid_user");
        req.setEmail("valid@example.com");
        req.setPassword("Password123!");
        return req;
    }

    private void stubHappyPathRegister(String username, String email) {
        when(repository.findByUsername(username)).thenReturn(Optional.empty());
        when(repository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(repository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ===================== REGISTER — null & blank =====================

    @Test
    void register_nullRequest_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(null));
        assertEquals("Request tidak boleh kosong!", ex.getMessage());
    }

    @Test
    void register_blankEmail_throwsException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("  ");
        req.setPassword("Password123!");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(req));
        assertEquals("Email dan password wajib diisi!", ex.getMessage());
    }

    @Test
    void register_blankPassword_throwsException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@example.com");
        req.setPassword("");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(req));
        assertEquals("Email dan password wajib diisi!", ex.getMessage());
    }

    // ===================== REGISTER — password rules =====================

    @Test
    void register_shortPassword_throwsException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@example.com");
        req.setPassword("Ab1!");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(req));
        assertEquals("Password minimal 8 karakter!", ex.getMessage());
    }

    @Test
    void register_weakPassword_noUppercase_throwsException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@example.com");
        req.setPassword("password123!");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(req));
        assertEquals(
                "Password harus mengandung huruf besar, huruf kecil, angka, dan karakter spesial (@$!%*?&_#)!",
                ex.getMessage());
    }

    @Test
    void register_weakPassword_noSpecialChar_throwsException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@example.com");
        req.setPassword("Password123");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(req));
        assertEquals(
                "Password harus mengandung huruf besar, huruf kecil, angka, dan karakter spesial (@$!%*?&_#)!",
                ex.getMessage());
    }

    // ===================== REGISTER — email & username =====================

    @Test
    void register_invalidEmailFormat_throwsException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("not-an-email");
        req.setPassword("Password123!");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(req));
        assertEquals("Format email tidak valid!", ex.getMessage());
    }

    @Test
    void register_invalidUsernameFormat_throwsException() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("invalid@user!");
        req.setEmail("user@example.com");
        req.setPassword("Password123!");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(req));
        assertEquals("Username hanya boleh berisi huruf, angka, dan underscore (3-30 karakter)!", ex.getMessage());
    }

    @Test
    void register_duplicateUsername_throwsException() {
        RegisterRequest req = validRegisterRequest();
        when(repository.findByUsername("valid_user")).thenReturn(Optional.of(new UserProfile()));
        when(repository.findByEmail("valid@example.com")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(req));
        assertEquals("Username atau Email sudah terdaftar!", ex.getMessage());
    }

    @Test
    void register_duplicateEmail_throwsException() {
        RegisterRequest req = validRegisterRequest();
        when(repository.findByUsername("valid_user")).thenReturn(Optional.empty());
        when(repository.findByEmail("valid@example.com")).thenReturn(Optional.of(new UserProfile()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(req));
        assertEquals("Username atau Email sudah terdaftar!", ex.getMessage());
    }

    // ===================== REGISTER — happy path & defaults =====================

    @Test
    void register_validData_returnsUser() {
        RegisterRequest req = validRegisterRequest();
        stubHappyPathRegister("valid_user", "valid@example.com");

        UserProfile result = authService.register(req);

        assertNotNull(result);
        assertEquals("valid_user", result.getUsername());
        assertEquals("valid@example.com", result.getEmail());
    }

    @Test
    void register_setsRoleAsTitiper() {
        RegisterRequest req = validRegisterRequest();
        stubHappyPathRegister("valid_user", "valid@example.com");

        UserProfile result = authService.register(req);

        assertEquals(UserRole.TITIPER.name(), result.getRole());
    }

    @Test
    void register_setsStatusAsActive() {
        RegisterRequest req = validRegisterRequest();
        stubHappyPathRegister("valid_user", "valid@example.com");

        UserProfile result = authService.register(req);

        assertEquals(AccountStatus.ACTIVE.name(), result.getAccountStatus());
    }

    @Test
    void register_setsKycAsNotSubmitted() {
        RegisterRequest req = validRegisterRequest();
        stubHappyPathRegister("valid_user", "valid@example.com");

        UserProfile result = authService.register(req);

        assertEquals(KycStatus.NOT_SUBMITTED.name(), result.getKycStatus());
    }

    @Test
    void register_encodesPassword() {
        RegisterRequest req = validRegisterRequest();
        stubHappyPathRegister("valid_user", "valid@example.com");

        UserProfile result = authService.register(req);

        assertEquals("hashed", result.getPassword());
        verify(passwordEncoder).encode("Password123!");
    }

    @Test
    void register_normalizesEmailToLowercase() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("some_user");
        req.setEmail("Mixed.Case@EXAMPLE.COM");
        req.setPassword("Password123!");

        stubHappyPathRegister("some_user", "mixed.case@example.com");

        UserProfile result = authService.register(req);

        assertEquals("mixed.case@example.com", result.getEmail());
    }

    @Test
    void register_autoGeneratesUsername_whenNotProvided() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("john.doe@example.com");
        req.setPassword("Password123!");

        when(repository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(repository.findByEmail("john.doe@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(repository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = authService.register(req);

        assertNotNull(result.getUsername());
        assertFalse(result.getUsername().isBlank());
    }

    @Test
    void register_autoGeneratesUniqueSuffix_whenBaseUsernameTaken() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("john.doe@gmail.com");
        req.setPassword("Password123!");

        when(repository.findByUsername("john_doe")).thenReturn(Optional.of(new UserProfile()));
        when(repository.findByUsername("john_doe_1")).thenReturn(Optional.empty());
        when(repository.findByEmail("john.doe@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(repository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = authService.register(req);

        assertEquals("john_doe_1", result.getUsername());
    }

    // ===================== LOGIN — null & blank =====================

    @Test
    void login_nullRequest_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(null));
        assertEquals("Request tidak boleh kosong!", ex.getMessage());
    }

    @Test
    void login_blankCredentials_throwsException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("");
        req.setPassword("");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(req));
        assertEquals("Email dan password wajib diisi!", ex.getMessage());
    }

    @Test
    void login_invalidEmailFormat_throwsException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("bukan-email");
        req.setPassword("Password123!");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(req));
        assertEquals("Format email tidak valid!", ex.getMessage());
    }

    // ===================== LOGIN — user lookup & password =====================

    @Test
    void login_emailNotFound_throwsException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("unknown@example.com");
        req.setPassword("Password123!");

        when(repository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(req));
        assertEquals("Email tidak ditemukan!", ex.getMessage());
    }

    @Test
    void login_wrongPassword_throwsException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@example.com");
        req.setPassword("WrongPass1!");

        UserProfile user = new UserProfile();
        user.setPassword("hashed_correct");
        user.setAccountStatus(AccountStatus.ACTIVE.name());

        when(repository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass1!", "hashed_correct")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(req));
        assertEquals("Password salah!", ex.getMessage());
    }

    @Test
    void login_bannedUser_throwsException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("banned@example.com");
        req.setPassword("Password123!");

        UserProfile user = new UserProfile();
        user.setPassword("hashed");
        user.setAccountStatus(AccountStatus.BANNED.name());

        when(repository.findByEmail("banned@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(req));
        assertEquals("Akun Anda telah di-ban!", ex.getMessage());
    }

    // ===================== LOGIN — happy path =====================

    @Test
    void login_validCredentials_returnsUser() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@example.com");
        req.setPassword("Password123!");

        UserProfile user = new UserProfile();
        user.setEmail("user@example.com");
        user.setPassword("hashed");
        user.setAccountStatus(AccountStatus.ACTIVE.name());

        when(repository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hashed")).thenReturn(true);

        UserProfile result = authService.login(req);

        assertNotNull(result);
        assertEquals("user@example.com", result.getEmail());
    }

    @Test
    void login_normalizesEmailToLowercase() {
        LoginRequest req = new LoginRequest();
        req.setEmail("USER@EXAMPLE.COM");
        req.setPassword("Password123!");

        UserProfile user = new UserProfile();
        user.setPassword("hashed");
        user.setAccountStatus(AccountStatus.ACTIVE.name());

        when(repository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        authService.login(req);

        verify(repository).findByEmail("user@example.com");
    }
}
