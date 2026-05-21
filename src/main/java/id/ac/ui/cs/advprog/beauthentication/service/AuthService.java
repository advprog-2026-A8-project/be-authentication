package id.ac.ui.cs.advprog.beauthentication.service;

import id.ac.ui.cs.advprog.beauthentication.dto.LoginRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.RegisterRequest;
import id.ac.ui.cs.advprog.beauthentication.model.AccountStatus;
import id.ac.ui.cs.advprog.beauthentication.model.KycStatus;
import id.ac.ui.cs.advprog.beauthentication.model.UserProfile;
import id.ac.ui.cs.advprog.beauthentication.model.UserRole;
import id.ac.ui.cs.advprog.beauthentication.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PASSWORD_COMPLEXITY_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_#]).{8,}$");

    private static final Pattern USERNAME_FORMAT_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]{3,30}$");

    @Autowired
    private UserProfileRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private String generateUsername(String normalizedEmail) {
        int atIndex = normalizedEmail.indexOf('@');
        String localPart = atIndex > 0 ? normalizedEmail.substring(0, atIndex) : normalizedEmail;
        String baseUsername = localPart.replaceAll("[^a-z0-9]", "_");

        if (baseUsername.isBlank()) {
            baseUsername = "user";
        }

        String candidate = baseUsername;
        int suffix = 1;

        while (repository.findByUsername(candidate).isPresent()) {
            candidate = baseUsername + "_" + suffix;
            suffix++;
        }

        return candidate;
    }

    public UserProfile register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request tidak boleh kosong!");
        }

        if (isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            throw new IllegalArgumentException("Email dan password wajib diisi!");
        }

        if (request.getPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password minimal 8 karakter!");
        }

        if (!PASSWORD_COMPLEXITY_PATTERN.matcher(request.getPassword()).matches()) {
            throw new IllegalArgumentException(
                    "Password harus mengandung huruf besar, huruf kecil, angka, dan karakter spesial (@$!%*?&_#)!");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (!isValidEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Format email tidak valid!");
        }

        String normalizedUsername;
        if (isBlank(request.getUsername())) {
            normalizedUsername = generateUsername(normalizedEmail);
        } else {
            normalizedUsername = request.getUsername().trim();
            if (!USERNAME_FORMAT_PATTERN.matcher(normalizedUsername).matches()) {
                throw new IllegalArgumentException(
                        "Username hanya boleh berisi huruf, angka, dan underscore (3-30 karakter)!");
            }
        }

        if (repository.findByUsername(normalizedUsername).isPresent() ||
                repository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Username atau Email sudah terdaftar!");
        }

        UserProfile user = new UserProfile();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setRole(UserRole.TITIPER.name());
        user.setAccountStatus(AccountStatus.ACTIVE.name());
        user.setKycStatus(KycStatus.NOT_SUBMITTED.name());

        return repository.save(user);
    }

    public UserProfile login(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request tidak boleh kosong!");
        }

        if (isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            throw new IllegalArgumentException("Email dan password wajib diisi!");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (!isValidEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Format email tidak valid!");
        }

        UserProfile user = repository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Email tidak ditemukan!"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Password salah!");
        }

        if (AccountStatus.BANNED.name().equals(user.getAccountStatus())) {
            throw new IllegalArgumentException("Akun Anda telah di-ban!");
        }

        return user;
    }
}