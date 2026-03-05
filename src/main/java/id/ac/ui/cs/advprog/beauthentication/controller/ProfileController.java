package id.ac.ui.cs.advprog.beauthentication.controller;

import id.ac.ui.cs.advprog.beauthentication.model.UserProfile;
import id.ac.ui.cs.advprog.beauthentication.repository.UserProfileRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private UserProfileRepository repository;

    @PostConstruct
    public void initDummyData() {
        if (repository.count() == 0) {
            UserProfile user1 = new UserProfile();
            user1.setUsername("naufal_muzaki");
            user1.setEmail("naufal@example.com");
            user1.setPassword("password123"); // Tambahan password dummy
            user1.setRole("CUSTOMER");

            UserProfile user2 = new UserProfile();
            user2.setUsername("asdos_reviewer");
            user2.setEmail("asdos@example.com");
            user2.setPassword("admin123"); // Tambahan password dummy
            user2.setRole("ADMIN");

            repository.saveAll(List.of(user1, user2));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserProfile>> getAllProfiles() {
        List<UserProfile> profiles = repository.findAll();
        return ResponseEntity.ok(profiles);
    }
}