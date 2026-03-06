package id.ac.ui.cs.advprog.beauthentication.service;

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

        user.setRole(request.getRole() != null ? request.getRole() : "CUSTOMER");

        return repository.save(user);
    }
}