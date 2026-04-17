package id.ac.ui.cs.advprog.beauthentication.service;

import id.ac.ui.cs.advprog.beauthentication.dto.KycSubmissionRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.beauthentication.model.KycStatus;
import id.ac.ui.cs.advprog.beauthentication.model.UserRole;
import id.ac.ui.cs.advprog.beauthentication.model.UserProfile;
import id.ac.ui.cs.advprog.beauthentication.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileService {

    @Autowired
    private UserProfileRepository repository;

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private UserProfile getByPrincipal(String principalIdentifier) {
        return repository.findByUsername(principalIdentifier)
                .or(() -> repository.findByEmail(principalIdentifier))
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan!"));
    }

    public UserProfile getByUsername(String username) {
        return getByPrincipal(username);
    }

    public UserProfile getByIdentifier(Long id, String username, String email) {
        int providedCount = 0;

        if (id != null) {
            providedCount++;
        }
        if (!isBlank(username)) {
            providedCount++;
        }
        if (!isBlank(email)) {
            providedCount++;
        }

        if (providedCount == 0) {
            throw new IllegalArgumentException("Salah satu identifier id, username, atau email wajib diisi!");
        }

        if (providedCount > 1) {
            throw new IllegalArgumentException("Gunakan tepat satu identifier: id, username, atau email.");
        }

        if (id != null) {
            return repository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan!"));
        }

        if (!isBlank(username)) {
            return repository.findByUsername(username.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan!"));
        }

        String normalizedEmail = email.trim().toLowerCase();
        return repository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan!"));
    }

    public List<UserProfile> getAllJastiperProfiles() {
        return repository.findAllByRole(UserRole.JASTIPER.name());
    }

    public UserProfile updateMyProfile(String principalIdentifier, UpdateProfileRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request tidak boleh kosong!");
        }

        UserProfile currentUser = getByPrincipal(principalIdentifier);

        if (request.getUsername() != null) {
            String newUsername = request.getUsername().trim();
            if (!newUsername.isBlank() && !newUsername.equals(currentUser.getUsername())) {
                if (repository.findByUsername(newUsername).isPresent()) {
                    throw new IllegalArgumentException("Username sudah terdaftar!");
                }
                currentUser.setUsername(newUsername);
            }
        }

        if (request.getFullName() != null) {
            currentUser.setFullName(request.getFullName().trim());
        }

        if (request.getPhoneNumber() != null) {
            currentUser.setPhoneNumber(request.getPhoneNumber().trim());
        }

        if (request.getBio() != null) {
            currentUser.setBio(request.getBio().trim());
        }

        return repository.save(currentUser);
    }

    public UserProfile submitKyc(String principalIdentifier, KycSubmissionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request tidak boleh kosong!");
        }

        if (isBlank(request.getFullName())
                || isBlank(request.getIdentityDocumentUrl())
                || isBlank(request.getSocialMediaUrl())) {
            throw new IllegalArgumentException("fullName, identityDocumentUrl, dan socialMediaUrl wajib diisi!");
        }

        UserProfile currentUser = getByPrincipal(principalIdentifier);
        currentUser.setFullName(request.getFullName().trim());
        currentUser.setKycIdentityDocumentUrl(request.getIdentityDocumentUrl().trim());
        currentUser.setKycSocialMediaUrl(request.getSocialMediaUrl().trim());
        currentUser.setKycStatus(KycStatus.PENDING.name());

        return repository.save(currentUser);
    }
}
