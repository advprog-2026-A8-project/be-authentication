package id.ac.ui.cs.advprog.beauthentication;

import id.ac.ui.cs.advprog.beauthentication.dto.BulkProfileLookupResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.KycDecisionResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.KycSubmissionRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.RoleDemoteResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.RoleUpgradeResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.AccountStatusUpdateResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.JastiperStatsUpdateResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.beauthentication.model.AccountStatus;
import id.ac.ui.cs.advprog.beauthentication.model.KycStatus;
import id.ac.ui.cs.advprog.beauthentication.model.UserProfile;
import id.ac.ui.cs.advprog.beauthentication.model.UserRole;
import id.ac.ui.cs.advprog.beauthentication.repository.UserProfileRepository;
import id.ac.ui.cs.advprog.beauthentication.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceUnitTests {

    @Mock
    private UserProfileRepository repository;

    @InjectMocks
    private ProfileService profileService;

    // ===================== HELPER =====================

    private UserProfile buildUser(String username, String email, String role,
                                  String kycStatus, String accountStatus) {
        UserProfile user = new UserProfile();
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        user.setKycStatus(kycStatus);
        user.setAccountStatus(accountStatus);
        return user;
    }

    private UserProfile titiperPending() {
        return buildUser("titiper", "titiper@example.com",
                UserRole.TITIPER.name(), KycStatus.PENDING.name(), AccountStatus.PENDING.name());
    }

    private UserProfile jastiperActive() {
        return buildUser("jastiper", "jastiper@example.com",
                UserRole.JASTIPER.name(), KycStatus.APPROVED.name(), AccountStatus.ACTIVE.name());
    }

    // ===================== getByIdentifier =====================

    @Test
    void getByIdentifier_withId_returnsUser() {
        UUID id = UUID.randomUUID();
        UserProfile user = buildUser("u", "u@e.com", UserRole.TITIPER.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        when(repository.findById(id)).thenReturn(Optional.of(user));

        UserProfile result = profileService.getByIdentifier(id, null, null);

        assertNotNull(result);
        assertEquals("u", result.getUsername());
    }

    @Test
    void getByIdentifier_withUsername_returnsUser() {
        UserProfile user = buildUser("target", "t@e.com", UserRole.TITIPER.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        when(repository.findByUsername("target")).thenReturn(Optional.of(user));

        UserProfile result = profileService.getByIdentifier(null, "target", null);

        assertEquals("target", result.getUsername());
    }

    @Test
    void getByIdentifier_withEmail_returnsUser() {
        UserProfile user = buildUser("u", "u@example.com", UserRole.TITIPER.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        when(repository.findByEmail("u@example.com")).thenReturn(Optional.of(user));

        UserProfile result = profileService.getByIdentifier(null, null, "U@EXAMPLE.COM");

        assertEquals("u", result.getUsername());
    }

    @Test
    void getByIdentifier_noIdentifier_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.getByIdentifier(null, null, null));
        assertEquals("Salah satu identifier id, username, atau email wajib diisi!", ex.getMessage());
    }

    @Test
    void getByIdentifier_multipleIdentifiers_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.getByIdentifier(UUID.randomUUID(), "user", null));
        assertEquals("Gunakan tepat satu identifier: id, username, atau email.", ex.getMessage());
    }

    @Test
    void getByIdentifier_idNotFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.getByIdentifier(id, null, null));
        assertEquals("Pengguna tidak ditemukan!", ex.getMessage());
    }

    // ===================== bulkLookupByIds =====================

    @Test
    void bulkLookup_allFound_returnsAllUsers() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UserProfile u1 = buildUser("user1", "u1@e.com", UserRole.TITIPER.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        u1.setId(id1);
        UserProfile u2 = buildUser("user2", "u2@e.com", UserRole.TITIPER.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        u2.setId(id2);
        when(repository.findAllById(any())).thenReturn(List.of(u1, u2));

        BulkProfileLookupResponse response = profileService.bulkLookupByIds(List.of(id1, id2));

        assertEquals(2, response.getUsers().size());
        assertTrue(response.getNotFoundIds().isEmpty());
    }

    @Test
    void bulkLookup_partialFound_returnsFoundAndNotFoundIds() {
        UUID foundId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();
        UserProfile u = buildUser("found", "f@e.com", UserRole.TITIPER.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        u.setId(foundId);
        when(repository.findAllById(any())).thenReturn(List.of(u));

        BulkProfileLookupResponse response = profileService.bulkLookupByIds(List.of(foundId, missingId));

        assertEquals(1, response.getUsers().size());
        assertEquals(1, response.getNotFoundIds().size());
        assertTrue(response.getNotFoundIds().contains(missingId));
    }

    @Test
    void bulkLookup_emptyList_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.bulkLookupByIds(Collections.emptyList()));
        assertEquals("userIds wajib diisi!", ex.getMessage());
    }

    @Test
    void bulkLookup_listWithNullElement_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.bulkLookupByIds(Arrays.asList(UUID.randomUUID(), null)));
        assertEquals("userIds tidak boleh berisi null!", ex.getMessage());
    }

    // ===================== upgradeRoleToJastiper =====================

    @Test
    void upgradeRole_nullUserId_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.upgradeRoleToJastiper(null));
        assertEquals("userId wajib diisi!", ex.getMessage());
    }

    @Test
    void upgradeRole_titiperWithApprovedKyc_upgrades() {
        UUID id = UUID.randomUUID();
        UserProfile user = buildUser("u", "u@e.com", UserRole.TITIPER.name(),
                KycStatus.APPROVED.name(), AccountStatus.ACTIVE.name());
        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RoleUpgradeResponse response = profileService.upgradeRoleToJastiper(id);

        assertEquals(UserRole.TITIPER.name(), response.getOldRole());
        assertEquals(UserRole.JASTIPER.name(), response.getNewRole());
    }

    @Test
    void upgradeRole_alreadyJastiper_returnsNoChange() {
        UUID id = UUID.randomUUID();
        UserProfile user = jastiperActive();
        when(repository.findById(id)).thenReturn(Optional.of(user));

        RoleUpgradeResponse response = profileService.upgradeRoleToJastiper(id);

        assertEquals(UserRole.JASTIPER.name(), response.getOldRole());
        assertEquals(UserRole.JASTIPER.name(), response.getNewRole());
        verify(repository, never()).save(any());
    }

    @Test
    void upgradeRole_notTitiper_throwsException() {
        UUID id = UUID.randomUUID();
        UserProfile admin = buildUser("admin", "a@e.com", UserRole.ADMIN.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        when(repository.findById(id)).thenReturn(Optional.of(admin));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.upgradeRoleToJastiper(id));
        assertEquals("Hanya user TITIPER yang dapat di-upgrade ke JASTIPER!", ex.getMessage());
    }

    @Test
    void upgradeRole_withoutApprovedKyc_throwsException() {
        UUID id = UUID.randomUUID();
        UserProfile user = buildUser("u", "u@e.com", UserRole.TITIPER.name(),
                KycStatus.PENDING.name(), AccountStatus.ACTIVE.name());
        when(repository.findById(id)).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.upgradeRoleToJastiper(id));
        assertEquals("Hanya user dengan KYC APPROVED yang dapat di-upgrade ke JASTIPER!", ex.getMessage());
    }

    // ===================== demoteRoleToTitiper =====================

    @Test
    void demoteRole_nullUserId_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.demoteRoleToTitiper(null));
        assertEquals("userId wajib diisi!", ex.getMessage());
    }

    @Test
    void demoteRole_jastiper_demotes() {
        UUID id = UUID.randomUUID();
        UserProfile user = jastiperActive();
        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RoleDemoteResponse response = profileService.demoteRoleToTitiper(id);

        assertEquals(UserRole.JASTIPER.name(), response.getOldRole());
        assertEquals(UserRole.TITIPER.name(), response.getNewRole());
    }

    @Test
    void demoteRole_alreadyTitiper_returnsNoChange() {
        UUID id = UUID.randomUUID();
        UserProfile user = buildUser("u", "u@e.com", UserRole.TITIPER.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        when(repository.findById(id)).thenReturn(Optional.of(user));

        RoleDemoteResponse response = profileService.demoteRoleToTitiper(id);

        assertEquals(UserRole.TITIPER.name(), response.getOldRole());
        assertEquals(UserRole.TITIPER.name(), response.getNewRole());
        verify(repository, never()).save(any());
    }

    @Test
    void demoteRole_admin_throwsException() {
        UUID id = UUID.randomUUID();
        UserProfile admin = buildUser("admin", "a@e.com", UserRole.ADMIN.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        when(repository.findById(id)).thenReturn(Optional.of(admin));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.demoteRoleToTitiper(id));
        assertEquals("Admin tidak dapat di-demote!", ex.getMessage());
    }

    // ===================== decideKyc =====================

    @Test
    void decideKyc_nullUserId_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.decideKyc(null, "APPROVE"));
        assertEquals("userId wajib diisi!", ex.getMessage());
    }

    @Test
    void decideKyc_blankDecision_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.decideKyc(UUID.randomUUID(), "  "));
        assertEquals("decision wajib diisi!", ex.getMessage());
    }

    @Test
    void decideKyc_invalidDecision_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.decideKyc(UUID.randomUUID(), "INVALID"));
        assertEquals("decision tidak valid!", ex.getMessage());
    }

    @Test
    void decideKyc_notPending_throwsException() {
        UUID id = UUID.randomUUID();
        UserProfile user = buildUser("u", "u@e.com", UserRole.TITIPER.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        when(repository.findById(id)).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.decideKyc(id, "APPROVE"));
        assertEquals("KYC hanya dapat diputuskan jika statusnya PENDING!", ex.getMessage());
    }

    @Test
    void decideKyc_approve_upgradesTitiperToJastiper() {
        UUID id = UUID.randomUUID();
        UserProfile user = titiperPending();
        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KycDecisionResponse response = profileService.decideKyc(id, "APPROVE");

        assertEquals(KycStatus.APPROVED.name(), response.getNewKycStatus());
        assertEquals(UserRole.JASTIPER.name(), response.getNewRole());
        assertEquals(AccountStatus.ACTIVE.name(), user.getAccountStatus());
    }

    @Test
    void decideKyc_approve_doesNotDowngradeJastiper() {
        UUID id = UUID.randomUUID();
        UserProfile user = buildUser("j", "j@e.com", UserRole.JASTIPER.name(),
                KycStatus.PENDING.name(), AccountStatus.PENDING.name());
        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KycDecisionResponse response = profileService.decideKyc(id, "APPROVE");

        assertEquals(UserRole.JASTIPER.name(), response.getOldRole());
        assertEquals(UserRole.JASTIPER.name(), response.getNewRole());
    }

    @Test
    void decideKyc_reject_keepsTitiperRole() {
        UUID id = UUID.randomUUID();
        UserProfile user = titiperPending();
        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KycDecisionResponse response = profileService.decideKyc(id, "REJECT");

        assertEquals(KycStatus.REJECTED.name(), response.getNewKycStatus());
        assertEquals(UserRole.TITIPER.name(), response.getNewRole());
        assertEquals(AccountStatus.ACTIVE.name(), user.getAccountStatus());
    }

    @Test
    void decideKyc_approvedAlias_worksLikeApprove() {
        UUID id = UUID.randomUUID();
        UserProfile user = titiperPending();
        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KycDecisionResponse response = profileService.decideKyc(id, "APPROVED");

        assertEquals(KycStatus.APPROVED.name(), response.getNewKycStatus());
        assertEquals(UserRole.JASTIPER.name(), response.getNewRole());
    }

    @Test
    void decideKyc_rejectedAlias_worksLikeReject() {
        UUID id = UUID.randomUUID();
        UserProfile user = titiperPending();
        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KycDecisionResponse response = profileService.decideKyc(id, "REJECTED");

        assertEquals(KycStatus.REJECTED.name(), response.getNewKycStatus());
        assertEquals(UserRole.TITIPER.name(), response.getNewRole());
    }

    // ===================== incrementSuccessfulTransactionCount =====================

    @Test
    void incrementStats_nullUserId_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.incrementSuccessfulTransactionCount(null, 1L));
        assertEquals("userId wajib diisi!", ex.getMessage());
    }

    @Test
    void incrementStats_positiveDelta_increments() {
        UUID id = UUID.randomUUID();
        UserProfile user = jastiperActive();
        user.setSuccessfulTransactionCount(5L);
        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JastiperStatsUpdateResponse response =
                profileService.incrementSuccessfulTransactionCount(id, 3L);

        assertEquals(5L, response.getOldCount());
        assertEquals(8L, response.getNewCount());
    }

    @Test
    void incrementStats_zeroDelta_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.incrementSuccessfulTransactionCount(UUID.randomUUID(), 0L));
        assertEquals("delta harus lebih besar dari 0!", ex.getMessage());
    }

    @Test
    void incrementStats_negativeDelta_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.incrementSuccessfulTransactionCount(UUID.randomUUID(), -1L));
        assertEquals("delta harus lebih besar dari 0!", ex.getMessage());
    }

    @Test
    void incrementStats_nonJastiper_throwsException() {
        UUID id = UUID.randomUUID();
        UserProfile user = buildUser("u", "u@e.com", UserRole.TITIPER.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        when(repository.findById(id)).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.incrementSuccessfulTransactionCount(id, 1L));
        assertEquals("Hanya JASTIPER yang dapat diupdate statistiknya!", ex.getMessage());
    }

    // ===================== updateAccountStatus =====================

    @Test
    void updateStatus_nullUserId_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.updateAccountStatus(null, "BANNED"));
        assertEquals("userId wajib diisi!", ex.getMessage());
    }

    @Test
    void updateStatus_toBanned_updates() {
        UUID id = UUID.randomUUID();
        UserProfile user = buildUser("u", "u@e.com", UserRole.TITIPER.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AccountStatusUpdateResponse response = profileService.updateAccountStatus(id, "BANNED");

        assertEquals(AccountStatus.ACTIVE.name(), response.getOldStatus());
        assertEquals(AccountStatus.BANNED.name(), response.getNewStatus());
    }

    @Test
    void updateStatus_sameStatus_doesNotSave() {
        UUID id = UUID.randomUUID();
        UserProfile user = buildUser("u", "u@e.com", UserRole.TITIPER.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        when(repository.findById(id)).thenReturn(Optional.of(user));

        profileService.updateAccountStatus(id, "ACTIVE");

        verify(repository, never()).save(any());
    }

    @Test
    void updateStatus_pendingStatus_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.updateAccountStatus(UUID.randomUUID(), "PENDING"));
        assertEquals("status tidak valid!", ex.getMessage());
    }

    @Test
    void updateStatus_invalidStatus_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.updateAccountStatus(UUID.randomUUID(), "UNKNOWN"));
        assertEquals("status tidak valid!", ex.getMessage());
    }

    // ===================== updateMyProfile =====================

    @Test
    void updateMyProfile_nullRequest_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.updateMyProfile("user@example.com", null));
        assertEquals("Request tidak boleh kosong!", ex.getMessage());
    }

    @Test
    void updateMyProfile_withFullName_updatesField() {
        UserProfile user = buildUser("user", "user@example.com", UserRole.TITIPER.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        when(repository.findByUsername("user@example.com")).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("Budi Santoso");

        UserProfile result = profileService.updateMyProfile("user@example.com", req);

        assertEquals("Budi Santoso", result.getFullName());
    }

    @Test
    void updateMyProfile_blankFullName_throwsException() {
        UserProfile user = buildUser("user", "user@example.com", UserRole.TITIPER.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        when(repository.findByUsername("user@example.com")).thenReturn(Optional.of(user));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.updateMyProfile("user@example.com", req));
        assertEquals("fullName wajib diisi!", ex.getMessage());
    }

    @Test
    void updateMyProfile_fullNameNotSetAndNotProvided_throwsException() {
        UserProfile user = buildUser("user", "user@example.com", UserRole.TITIPER.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        when(repository.findByUsername("user@example.com")).thenReturn(Optional.of(user));

        UpdateProfileRequest req = new UpdateProfileRequest();
        // fullName intentionally null — and user doesn't have a fullName yet

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.updateMyProfile("user@example.com", req));
        assertEquals("fullName wajib diisi!", ex.getMessage());
    }

    @Test
    void updateMyProfile_duplicateUsername_throwsException() {
        UserProfile user = buildUser("current_user", "u@example.com", UserRole.TITIPER.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        user.setFullName("Nama Ada");
        when(repository.findByUsername("u@example.com")).thenReturn(Optional.of(user));
        when(repository.findByUsername("taken_user")).thenReturn(Optional.of(new UserProfile()));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setUsername("taken_user");
        req.setFullName("Nama Ada");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.updateMyProfile("u@example.com", req));
        assertEquals("Username sudah terdaftar!", ex.getMessage());
    }

    @Test
    void updateMyProfile_blankUsername_keepsCurrentUsername() {
        UserProfile user = buildUser("current_user", "u@example.com", UserRole.TITIPER.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        user.setFullName("Nama Ada");
        when(repository.findByUsername("u@example.com")).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setUsername("   ");
        req.setFullName("Nama Ada");

        UserProfile result = profileService.updateMyProfile("u@example.com", req);

        assertEquals("current_user", result.getUsername());
    }

    @Test
    void updateMyProfile_invalidPhone_throwsException() {
        UserProfile user = buildUser("user", "u@example.com", UserRole.TITIPER.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        user.setFullName("Nama Ada");
        when(repository.findByUsername("u@example.com")).thenReturn(Optional.of(user));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("Nama Ada");
        req.setPhoneNumber("abc-bukan-nomor");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.updateMyProfile("u@example.com", req));
        assertEquals("Nomor telepon tidak valid!", ex.getMessage());
    }

    @Test
    void updateMyProfile_validPhone_updatesPhone() {
        UserProfile user = buildUser("user", "u@example.com", UserRole.TITIPER.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        user.setFullName("Nama Ada");
        when(repository.findByUsername("u@example.com")).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("Nama Ada");
        req.setPhoneNumber("+6281234567890");

        UserProfile result = profileService.updateMyProfile("u@example.com", req);

        assertEquals("+6281234567890", result.getPhoneNumber());
    }

    // ===================== submitKyc =====================

    @Test
    void submitKyc_nullRequest_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.submitKyc("user@example.com", null));
        assertEquals("Request tidak boleh kosong!", ex.getMessage());
    }

    @Test
    void submitKyc_missingFullName_throwsException() {
        KycSubmissionRequest req = new KycSubmissionRequest();
        req.setIdentityDocumentUrl("https://doc.example.com");
        req.setSocialMediaUrl("https://ig.com/user");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.submitKyc("user@example.com", req));
        assertEquals("fullName, identityDocumentUrl, dan socialMediaUrl wajib diisi!", ex.getMessage());
    }

    @Test
    void submitKyc_invalidDocUrl_throwsException() {
        KycSubmissionRequest req = new KycSubmissionRequest();
        req.setFullName("Budi Santoso");
        req.setIdentityDocumentUrl("bukan-url");
        req.setSocialMediaUrl("https://ig.com/budi");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.submitKyc("user@example.com", req));
        assertEquals("URL harus diawali dengan http:// atau https://", ex.getMessage());
    }

    @Test
    void submitKyc_invalidSocialUrl_throwsException() {
        KycSubmissionRequest req = new KycSubmissionRequest();
        req.setFullName("Budi Santoso");
        req.setIdentityDocumentUrl("https://doc.example.com/ktp");
        req.setSocialMediaUrl("instagram.com/budi");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.submitKyc("user@example.com", req));
        assertEquals("URL harus diawali dengan http:// atau https://", ex.getMessage());
    }

    @Test
    void submitKyc_alreadyApproved_throwsException() {
        UserProfile user = buildUser("user", "user@example.com", UserRole.JASTIPER.name(),
                KycStatus.APPROVED.name(), AccountStatus.ACTIVE.name());
        when(repository.findByUsername("user@example.com")).thenReturn(Optional.of(user));

        KycSubmissionRequest req = new KycSubmissionRequest();
        req.setFullName("Budi Santoso");
        req.setIdentityDocumentUrl("https://doc.example.com/ktp");
        req.setSocialMediaUrl("https://ig.com/budi");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.submitKyc("user@example.com", req));
        assertEquals("KYC sudah disetujui dan tidak dapat diajukan ulang!", ex.getMessage());
    }

    @Test
    void submitKyc_validData_setsPendingStatus() {
        UserProfile user = buildUser("user", "user@example.com", UserRole.TITIPER.name(),
                KycStatus.NOT_SUBMITTED.name(), AccountStatus.ACTIVE.name());
        when(repository.findByUsername("user@example.com")).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KycSubmissionRequest req = new KycSubmissionRequest();
        req.setFullName("Budi Santoso");
        req.setIdentityDocumentUrl("https://doc.example.com/ktp");
        req.setSocialMediaUrl("https://ig.com/budi");

        UserProfile result = profileService.submitKyc("user@example.com", req);

        assertEquals(KycStatus.PENDING.name(), result.getKycStatus());
        assertEquals(AccountStatus.PENDING.name(), result.getAccountStatus());
        assertEquals("Budi Santoso", result.getFullName());
    }
}
