package id.ac.ui.cs.advprog.beauthentication.controller;

import id.ac.ui.cs.advprog.beauthentication.dto.AccountStatusUpdateRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.AccountStatusUpdateResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.ApiResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.BulkProfileLookupRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.BulkProfileLookupResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.JastiperStatsUpdateRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.JastiperStatsUpdateResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.KycDecisionRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.KycDecisionResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.KycSubmissionRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.KycSubmissionResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.ProfileResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.PublicProfileResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.RoleDemoteRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.RoleDemoteResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.RoleUpgradeRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.RoleUpgradeResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.beauthentication.model.UserProfile;
import id.ac.ui.cs.advprog.beauthentication.repository.UserProfileRepository;
import id.ac.ui.cs.advprog.beauthentication.service.IProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private UserProfileRepository repository;

    @Autowired
    private IProfileService profileService;

    private ProfileResponse toProfileResponse(UserProfile user) {
        return new ProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getBio(),
                user.getRole(),
                user.getAccountStatus(),
                user.getKycStatus(),
                user.getSuccessfulTransactionCount()
        );
    }

    private String resolveUsername(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof String principalName) {
            return principalName;
        }
        return authentication.getName();
    }

    private PublicProfileResponse toPublicProfileResponse(UserProfile user) {
        return new PublicProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getBio(),
                user.getRole(),
                user.getKycStatus(),
                user.getSuccessfulTransactionCount()
        );
    }

    private boolean isNotFound(String message) {
        return "Pengguna tidak ditemukan!".equals(message);
    }

    private KycSubmissionResponse toKycSubmissionResponse(UserProfile user) {
        return new KycSubmissionResponse(
                user.getFullName(),
                user.getKycIdentityDocumentUrl(),
                user.getKycSocialMediaUrl(),
                user.getKycStatus()
        );
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ProfileResponse>>> getAllProfiles() {
        List<ProfileResponse> profiles = repository.findAll().stream()
                .map(this::toProfileResponse)
                .toList();
        return ResponseEntity.ok(new ApiResponse<>("Daftar profil berhasil diambil!", profiles));
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ProfileResponse>>> getAllProfilesForAdmin() {
        List<ProfileResponse> profiles = profileService.getAllProfiles().stream()
                .map(this::toProfileResponse)
                .toList();
        return ResponseEntity.ok(new ApiResponse<>("Daftar profil berhasil diambil!", profiles));
    }

    @GetMapping("/jastiper")
    public ResponseEntity<ApiResponse<List<PublicProfileResponse>>> getAllJastiperProfiles() {
        List<PublicProfileResponse> profiles = profileService.getAllJastiperProfiles().stream()
                .map(this::toPublicProfileResponse)
                .toList();
        return ResponseEntity.ok(new ApiResponse<>("Daftar jastiper berhasil diambil!", profiles));
    }

    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<PublicProfileResponse>> lookupProfile(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email
    ) {
        try {
            UserProfile user = profileService.getByIdentifier(id, username, email);
            return ResponseEntity.ok(new ApiResponse<>("Profil berhasil ditemukan!", toPublicProfileResponse(user)));
        } catch (IllegalArgumentException e) {
            HttpStatus status = isNotFound(e.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status)
                    .body(new ApiResponse<>(e.getMessage(), null));
        }
    }

    @PostMapping("/lookup/bulk")
    public ResponseEntity<ApiResponse<BulkProfileLookupResponse>> bulkLookupProfile(
            @RequestBody BulkProfileLookupRequest request
    ) {
        try {
            List<UUID> userIds = request == null ? null : request.getUserIds();
            BulkProfileLookupResponse response = profileService.bulkLookupByIds(userIds);
            return ResponseEntity.ok(new ApiResponse<>("Bulk lookup profil berhasil!", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(e.getMessage(), null));
        }
    }

    @PutMapping("/admin/role/upgrade")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleUpgradeResponse>> upgradeRoleToJastiper(
            @RequestBody RoleUpgradeRequest request
    ) {
        try {
            UUID userId = request == null ? null : request.getUserId();
            RoleUpgradeResponse response = profileService.upgradeRoleToJastiper(userId);
            return ResponseEntity.ok(new ApiResponse<>("Role user berhasil di-upgrade ke JASTIPER!", response));
        } catch (IllegalArgumentException e) {
            HttpStatus status = isNotFound(e.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status)
                    .body(new ApiResponse<>(e.getMessage(), null));
        }
    }

    @PutMapping("/admin/role/demote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleDemoteResponse>> demoteRoleToTitiper(
            @RequestBody RoleDemoteRequest request
    ) {
        try {
            UUID userId = request == null ? null : request.getUserId();
            RoleDemoteResponse response = profileService.demoteRoleToTitiper(userId);
            return ResponseEntity.ok(new ApiResponse<>("Role user berhasil di-demote ke TITIPER!", response));
        } catch (IllegalArgumentException e) {
            HttpStatus status = isNotFound(e.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status)
                    .body(new ApiResponse<>(e.getMessage(), null));
        }
    }

    @PutMapping("/admin/kyc/decision")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<KycDecisionResponse>> decideKyc(
            @RequestBody KycDecisionRequest request
    ) {
        try {
            UUID userId = request == null ? null : request.getUserId();
            String decision = request == null ? null : request.getDecision();
            KycDecisionResponse response = profileService.decideKyc(userId, decision);
            return ResponseEntity.ok(new ApiResponse<>("Keputusan KYC berhasil diproses!", response));
        } catch (IllegalArgumentException e) {
            HttpStatus status = isNotFound(e.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status)
                    .body(new ApiResponse<>(e.getMessage(), null));
        }
    }

    @PutMapping("/admin/jastiper/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<JastiperStatsUpdateResponse>> updateJastiperStats(
            @RequestBody JastiperStatsUpdateRequest request
    ) {
        try {
            UUID userId = request == null ? null : request.getUserId();
            Long delta = request == null ? null : request.getDelta();
            JastiperStatsUpdateResponse response = profileService.incrementSuccessfulTransactionCount(userId, delta);
            return ResponseEntity.ok(new ApiResponse<>("Statistik Jastiper berhasil diperbarui!", response));
        } catch (IllegalArgumentException e) {
            HttpStatus status = isNotFound(e.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status)
                    .body(new ApiResponse<>(e.getMessage(), null));
        }
    }

    @PutMapping("/admin/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccountStatusUpdateResponse>> updateAccountStatus(
            @RequestBody AccountStatusUpdateRequest request
    ) {
        try {
            UUID userId = request == null ? null : request.getUserId();
            String status = request == null ? null : request.getStatus();
            AccountStatusUpdateResponse response = profileService.updateAccountStatus(userId, status);
            return ResponseEntity.ok(new ApiResponse<>("Status akun berhasil diperbarui!", response));
        } catch (IllegalArgumentException e) {
            HttpStatus status = isNotFound(e.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status)
                    .body(new ApiResponse<>(e.getMessage(), null));
        }
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

    @PostMapping("/kyc/submit")
    public ResponseEntity<ApiResponse<KycSubmissionResponse>> submitKyc(
            @RequestBody KycSubmissionRequest request,
            Authentication authentication
    ) {
        try {
            String username = resolveUsername(authentication);
            UserProfile updated = profileService.submitKyc(username, request);
            return ResponseEntity.ok(new ApiResponse<>("Pengajuan KYC berhasil dikirim!", toKycSubmissionResponse(updated)));
        } catch (IllegalArgumentException e) {
            HttpStatus status = isNotFound(e.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status)
                    .body(new ApiResponse<>(e.getMessage(), null));
        }
    }
}