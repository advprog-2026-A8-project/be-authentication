package id.ac.ui.cs.advprog.beauthentication.service;

import id.ac.ui.cs.advprog.beauthentication.dto.AccountStatusUpdateResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.BulkProfileLookupResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.JastiperStatsUpdateResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.KycDecisionResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.KycSubmissionRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.RoleDemoteResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.RoleUpgradeResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.UserLookupSummaryResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.beauthentication.model.AccountStatus;
import id.ac.ui.cs.advprog.beauthentication.model.KycStatus;
import id.ac.ui.cs.advprog.beauthentication.model.UserRole;
import id.ac.ui.cs.advprog.beauthentication.model.UserProfile;
import id.ac.ui.cs.advprog.beauthentication.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private static final String KYC_APPROVE = "APPROVE";
    private static final String KYC_REJECT = "REJECT";

    @Autowired
    private UserProfileRepository repository;

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private UserLookupSummaryResponse toLookupSummary(UserProfile user) {
        return new UserLookupSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getKycStatus()
        );
    }

    private UserProfile getByPrincipal(String principalIdentifier) {
        return repository.findByUsername(principalIdentifier)
                .or(() -> repository.findByEmail(principalIdentifier))
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan!"));
    }

    public UserProfile getByUsername(String username) {
        return getByPrincipal(username);
    }

    public UserProfile getByIdentifier(UUID id, String username, String email) {
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

    public List<UserProfile> getAllProfiles() {
        return repository.findAll();
    }

    public BulkProfileLookupResponse bulkLookupByIds(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("userIds wajib diisi!");
        }

        if (userIds.stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException("userIds tidak boleh berisi null!");
        }

        LinkedHashSet<UUID> uniqueIds = new LinkedHashSet<>(userIds);
        Map<UUID, UserProfile> profileMap = repository.findAllById(uniqueIds).stream()
                .collect(Collectors.toMap(UserProfile::getId, Function.identity()));

        List<UserLookupSummaryResponse> users = new ArrayList<>();
        List<UUID> notFoundIds = new ArrayList<>();

        for (UUID id : uniqueIds) {
            UserProfile user = profileMap.get(id);
            if (user == null) {
                notFoundIds.add(id);
            } else {
                users.add(toLookupSummary(user));
            }
        }

        return new BulkProfileLookupResponse(users, notFoundIds);
    }

    public RoleUpgradeResponse upgradeRoleToJastiper(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId wajib diisi!");
        }

        UserProfile user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan!"));

        String oldRole = user.getRole();

        if (UserRole.JASTIPER.name().equals(oldRole)) {
            return new RoleUpgradeResponse(user.getId(), oldRole, user.getRole());
        }

        if (!UserRole.TITIPER.name().equals(oldRole)) {
            throw new IllegalArgumentException("Hanya user TITIPER yang dapat di-upgrade ke JASTIPER!");
        }

        if (!KycStatus.APPROVED.name().equals(user.getKycStatus())) {
            throw new IllegalArgumentException("Hanya user dengan KYC APPROVED yang dapat di-upgrade ke JASTIPER!");
        }

        user.setRole(UserRole.JASTIPER.name());
        UserProfile updated = repository.save(user);

        return new RoleUpgradeResponse(updated.getId(), oldRole, updated.getRole());
    }

    public RoleDemoteResponse demoteRoleToTitiper(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId wajib diisi!");
        }

        UserProfile user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan!"));

        String oldRole = user.getRole();

        if (UserRole.ADMIN.name().equals(oldRole)) {
            throw new IllegalArgumentException("Admin tidak dapat di-demote!");
        }

        if (UserRole.TITIPER.name().equals(oldRole)) {
            return new RoleDemoteResponse(user.getId(), oldRole, user.getRole());
        }

        if (!UserRole.JASTIPER.name().equals(oldRole)) {
            throw new IllegalArgumentException("Role user tidak valid!");
        }

        user.setRole(UserRole.TITIPER.name());
        UserProfile updated = repository.save(user);

        return new RoleDemoteResponse(updated.getId(), oldRole, updated.getRole());
    }

    public KycDecisionResponse decideKyc(UUID userId, String decision) {
        if (userId == null) {
            throw new IllegalArgumentException("userId wajib diisi!");
        }

        if (isBlank(decision)) {
            throw new IllegalArgumentException("decision wajib diisi!");
        }

        String normalizedDecision = decision.trim().toUpperCase(Locale.ROOT);
        if (!KYC_APPROVE.equals(normalizedDecision) && !KYC_REJECT.equals(normalizedDecision)) {
            throw new IllegalArgumentException("decision tidak valid!");
        }

        UserProfile user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan!"));

        if (!KycStatus.PENDING.name().equals(user.getKycStatus())) {
            throw new IllegalArgumentException("KYC hanya dapat diputuskan jika statusnya PENDING!");
        }

        String oldKycStatus = user.getKycStatus();
        String oldRole = user.getRole();

        if (KYC_APPROVE.equals(normalizedDecision)) {
            user.setKycStatus(KycStatus.APPROVED.name());
            user.setAccountStatus(AccountStatus.ACTIVE.name());
            if (UserRole.TITIPER.name().equals(oldRole)) {
                user.setRole(UserRole.JASTIPER.name());
            }
        } else {
            user.setKycStatus(KycStatus.REJECTED.name());
            user.setAccountStatus(AccountStatus.ACTIVE.name());
        }

        UserProfile updated = repository.save(user);
        return new KycDecisionResponse(
                updated.getId(),
                oldKycStatus,
                updated.getKycStatus(),
                oldRole,
                updated.getRole()
        );
    }

    public JastiperStatsUpdateResponse incrementSuccessfulTransactionCount(UUID userId, Long delta) {
        if (userId == null) {
            throw new IllegalArgumentException("userId wajib diisi!");
        }

        if (delta == null) {
            throw new IllegalArgumentException("delta wajib diisi!");
        }

        if (delta <= 0) {
            throw new IllegalArgumentException("delta harus lebih besar dari 0!");
        }

        UserProfile user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan!"));

        if (!UserRole.JASTIPER.name().equals(user.getRole())) {
            throw new IllegalArgumentException("Hanya JASTIPER yang dapat diupdate statistiknya!");
        }

        Long oldCount = user.getSuccessfulTransactionCount();
        if (oldCount == null) {
            oldCount = 0L;
        }

        long newCount = oldCount + delta;
        user.setSuccessfulTransactionCount(newCount);
        UserProfile updated = repository.save(user);

        return new JastiperStatsUpdateResponse(updated.getId(), oldCount, updated.getSuccessfulTransactionCount());
    }

    public AccountStatusUpdateResponse updateAccountStatus(UUID userId, String status) {
        if (userId == null) {
            throw new IllegalArgumentException("userId wajib diisi!");
        }

        if (isBlank(status)) {
            throw new IllegalArgumentException("status wajib diisi!");
        }

        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        AccountStatus targetStatus;

        try {
            targetStatus = AccountStatus.valueOf(normalizedStatus);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("status tidak valid!");
        }

        if (AccountStatus.PENDING == targetStatus) {
            throw new IllegalArgumentException("status tidak valid!");
        }

        UserProfile user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan!"));

        String oldStatus = user.getAccountStatus();

        if (!targetStatus.name().equalsIgnoreCase(oldStatus)) {
            user.setAccountStatus(targetStatus.name());
            user = repository.save(user);
        }

        return new AccountStatusUpdateResponse(user.getId(), oldStatus, user.getAccountStatus());
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
            String trimmedFullName = request.getFullName().trim();
            if (trimmedFullName.isBlank()) {
                throw new IllegalArgumentException("fullName wajib diisi!");
            }
            currentUser.setFullName(trimmedFullName);
        } else if (isBlank(currentUser.getFullName())) {
            throw new IllegalArgumentException("fullName wajib diisi!");
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

        if (KycStatus.APPROVED.name().equals(currentUser.getKycStatus())) {
            throw new IllegalArgumentException("KYC sudah disetujui dan tidak dapat diajukan ulang!");
        }

        currentUser.setFullName(request.getFullName().trim());
        currentUser.setKycIdentityDocumentUrl(request.getIdentityDocumentUrl().trim());
        currentUser.setKycSocialMediaUrl(request.getSocialMediaUrl().trim());
        currentUser.setKycStatus(KycStatus.PENDING.name());
        currentUser.setAccountStatus(AccountStatus.PENDING.name());

        return repository.save(currentUser);
    }
}
