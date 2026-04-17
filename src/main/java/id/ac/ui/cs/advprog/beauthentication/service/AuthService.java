package id.ac.ui.cs.advprog.beauthentication.service;

import id.ac.ui.cs.advprog.beauthentication.dto.LoginRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.RegisterRequest;
import id.ac.ui.cs.advprog.beauthentication.model.KycStatus;
import id.ac.ui.cs.advprog.beauthentication.model.UserProfile;
import id.ac.ui.cs.advprog.beauthentication.model.UserRole;
import id.ac.ui.cs.advprog.beauthentication.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    @Autowired
    private UserProfileRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isValidEmail(String email) {
        int atIndex = email.indexOf('@');
        int dotIndex = email.lastIndexOf('.');
        return atIndex > 0 && dotIndex > atIndex + 1 && dotIndex < email.length() - 1;
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

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (!isValidEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Format email tidak valid!");
        }

        String normalizedUsername = isBlank(request.getUsername())
                ? generateUsername(normalizedEmail)
                : request.getUsername().trim();

        if (repository.findByUsername(normalizedUsername).isPresent() ||
                repository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Username atau Email sudah terdaftar!");
        }

        UserProfile user = new UserProfile();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setRole(UserRole.TITIPER.name());
        user.setKycStatus(KycStatus.PENDING.name());

        return repository.save(user);
    }

    public UserProfile login(LoginRequest request){
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

        // cari user dari email
        UserProfile user = repository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new IllegalArgumentException("Email tidak ditemukan!"));

        // cocokkan password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Password salah!");
        }

        return user;
    }
}