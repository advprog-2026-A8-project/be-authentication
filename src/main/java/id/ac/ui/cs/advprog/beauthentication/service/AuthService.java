package id.ac.ui.cs.advprog.beauthentication.service;

import id.ac.ui.cs.advprog.beauthentication.dto.LoginRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.RegisterRequest;
import id.ac.ui.cs.advprog.beauthentication.model.UserProfile;
import id.ac.ui.cs.advprog.beauthentication.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserProfileRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserProfile register(RegisterRequest request) {
        if (repository.findByUsername(request.getUsername()).isPresent() ||
                repository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Username atau Email sudah terdaftar!");
        }

        UserProfile user = new UserProfile();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setRole("CUSTOMER");

        return repository.save(user);
    }

    public UserProfile login(LoginRequest request){
        // cari user dari username
        UserProfile user = repository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Username tidak ditemukan!"));

        // cocokkan password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Password salah!");
        }

        return user;
    }
}