package id.ac.ui.cs.advprog.beauthentication.repository;

import id.ac.ui.cs.advprog.beauthentication.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUsername(String username);
    Optional<UserProfile> findByEmail(String email);
    List<UserProfile> findAllByRole(String role);
}
