package id.ac.ui.cs.advprog.beauthentication.config;

import id.ac.ui.cs.advprog.beauthentication.model.AccountStatus;
import id.ac.ui.cs.advprog.beauthentication.model.KycStatus;
import id.ac.ui.cs.advprog.beauthentication.model.UserProfile;
import id.ac.ui.cs.advprog.beauthentication.model.UserRole;
import id.ac.ui.cs.advprog.beauthentication.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserProfileRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (repository.count() == 0) {
            UserProfile user1 = new UserProfile();
            user1.setUsername("naufal_muzaki");
            user1.setEmail("naufal@example.com");
            user1.setPassword(passwordEncoder.encode("password123"));
            user1.setRole(UserRole.JASTIPER.name());
            user1.setAccountStatus(AccountStatus.ACTIVE.name());
            user1.setKycStatus(KycStatus.PENDING.name());

            UserProfile user2 = new UserProfile();
            user2.setUsername("asdos_reviewer");
            user2.setEmail("asdos@example.com");
            user2.setPassword(passwordEncoder.encode("admin123"));
            user2.setRole(UserRole.ADMIN.name());
            user2.setAccountStatus(AccountStatus.ACTIVE.name());
            user2.setKycStatus(KycStatus.APPROVED.name());

            repository.saveAll(List.of(user1, user2));
        }
    }
}