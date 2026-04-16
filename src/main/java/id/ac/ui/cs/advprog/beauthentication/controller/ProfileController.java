package id.ac.ui.cs.advprog.beauthentication.controller;

import id.ac.ui.cs.advprog.beauthentication.dto.ApiResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.ProfileResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.beauthentication.model.UserProfile;
import id.ac.ui.cs.advprog.beauthentication.repository.UserProfileRepository;
import id.ac.ui.cs.advprog.beauthentication.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private UserProfileRepository repository;

    @Autowired
    private ProfileService profileService;

    private ProfileResponse toProfileResponse(UserProfile user) {
        return new ProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getBio(),
                user.getRole(),
                user.getKycStatus()
        );
    }

    private String resolveUsername(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof String principalName) {
            return principalName;
        }
        return authentication.getName();
    }

    private boolean isNotFound(String message) {
        return "Pengguna tidak ditemukan!".equals(message);
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserProfile>> getAllProfiles() {
        List<UserProfile> profiles = repository.findAll();
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(Authentication authentication) {
        try {
            String username = resolveUsername(authentication);
            UserProfile user = profileService.getByUsername(username);
            return ResponseEntity.ok(new ApiResponse<>("Profil berhasil diambil!", toProfileResponse(user)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(e.getMessage(), null));
        }
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateMyProfile(
            @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        try {
            String username = resolveUsername(authentication);
            UserProfile updated = profileService.updateMyProfile(username, request);
            return ResponseEntity.ok(new ApiResponse<>("Profil berhasil diperbarui!", toProfileResponse(updated)));
        } catch (IllegalArgumentException e) {
            HttpStatus status = isNotFound(e.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status)
                    .body(new ApiResponse<>(e.getMessage(), null));
        }
    }
}