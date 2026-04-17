package id.ac.ui.cs.advprog.beauthentication;

import id.ac.ui.cs.advprog.beauthentication.dto.BulkProfileLookupResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.KycSubmissionRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.RoleUpgradeResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.UpdateProfileRequest;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceUnitTests {

    @Mock
    private UserProfileRepository repository;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void shouldGetByUsernameWhenPrincipalMatchesUsername() {
        UserProfile user = user(1L, "alice", "alice@example.com", UserRole.TITIPER.name(), KycStatus.PENDING.name());
        when(repository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserProfile result = profileService.getByUsername("alice");

        assertSame(user, result);
        verify(repository, never()).findByEmail(any());
    }

    @Test
    void shouldGetByUsernameUsingEmailFallback() {
        UserProfile user = user(1L, "alice", "alice@example.com", UserRole.TITIPER.name(), KycStatus.PENDING.name());
        when(repository.findByUsername("alice@example.com")).thenReturn(Optional.empty());
        when(repository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        UserProfile result = profileService.getByUsername("alice@example.com");

        assertSame(user, result);
    }

    @Test
    void shouldRejectGetByUsernameWhenUserNotFound() {
        when(repository.findByUsername("missing")).thenReturn(Optional.empty());
        when(repository.findByEmail("missing")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.getByUsername("missing"));

        assertEquals("Pengguna tidak ditemukan!", ex.getMessage());
    }

    @Test
    void shouldRejectIdentifierLookupWhenNoIdentifierProvided() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.getByIdentifier(null, null, null));

        assertEquals("Salah satu identifier id, username, atau email wajib diisi!", ex.getMessage());
    }

    @Test
    void shouldRejectIdentifierLookupWhenMultipleIdentifiersProvided() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.getByIdentifier(1L, "alice", null));

        assertEquals("Gunakan tepat satu identifier: id, username, atau email.", ex.getMessage());
    }

    @Test
    void shouldLookupByIdSuccessfully() {
        UserProfile user = user(10L, "alice", "alice@example.com", UserRole.TITIPER.name(), KycStatus.PENDING.name());
        when(repository.findById(10L)).thenReturn(Optional.of(user));

        UserProfile result = profileService.getByIdentifier(10L, null, null);

        assertSame(user, result);
    }

    @Test
    void shouldLookupByUsernameWithTrim() {
        UserProfile user = user(11L, "alice", "alice@example.com", UserRole.TITIPER.name(), KycStatus.PENDING.name());
        when(repository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserProfile result = profileService.getByIdentifier(null, "  alice  ", null);

        assertSame(user, result);
    }

    @Test
    void shouldLookupByNormalizedEmail() {
        UserProfile user = user(12L, "alice", "alice@example.com", UserRole.TITIPER.name(), KycStatus.PENDING.name());
        when(repository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        UserProfile result = profileService.getByIdentifier(null, null, "  ALICE@Example.com  ");

        assertSame(user, result);
    }

    @Test
    void shouldReturnAllJastiperProfiles() {
        List<UserProfile> expected = List.of(
                user(1L, "jastiper_a", "a@example.com", UserRole.JASTIPER.name(), KycStatus.PENDING.name()),
                user(2L, "jastiper_b", "b@example.com", UserRole.JASTIPER.name(), KycStatus.PENDING.name())
        );
        when(repository.findAllByRole(UserRole.JASTIPER.name())).thenReturn(expected);

        List<UserProfile> result = profileService.getAllJastiperProfiles();

        assertEquals(2, result.size());
        assertEquals("jastiper_a", result.get(0).getUsername());
    }

    @Test
    void shouldRejectBulkLookupWhenIdsNullOrEmpty() {
        IllegalArgumentException nullEx = assertThrows(IllegalArgumentException.class,
                () -> profileService.bulkLookupByIds(null));
        IllegalArgumentException emptyEx = assertThrows(IllegalArgumentException.class,
                () -> profileService.bulkLookupByIds(List.of()));

        assertEquals("userIds wajib diisi!", nullEx.getMessage());
        assertEquals("userIds wajib diisi!", emptyEx.getMessage());
    }

    @Test
    void shouldRejectBulkLookupWhenContainsNull() {
        List<Long> ids = new ArrayList<>();
        ids.add(1L);
        ids.add(null);
        ids.add(2L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> profileService.bulkLookupByIds(ids));

        assertEquals("userIds tidak boleh berisi null!", ex.getMessage());
    }

    @Test
    void shouldBulkLookupDeduplicateAndTrackMissingIds() {
        UserProfile one = user(1L, "u1", "u1@example.com", UserRole.TITIPER.name(), KycStatus.PENDING.name());
        UserProfile three = user(3L, "u3", "u3@example.com", UserRole.ADMIN.name(), KycStatus.PENDING.name());

        when(repository.findAllById(any())).thenReturn(List.of(one, three));

        BulkProfileLookupResponse result = profileService.bulkLookupByIds(List.of(1L, 2L, 1L, 3L));

        assertEquals(2, result.getUsers().size());
        assertEquals(1L, result.getUsers().get(0).getId());
        assertEquals(3L, result.getUsers().get(1).getId());
        assertEquals(List.of(2L), result.getNotFoundIds());
    }

    @Test
    void shouldRejectRoleUpgradeWhenUserIdNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.upgradeRoleToJastiper(null));

        assertEquals("userId wajib diisi!", ex.getMessage());
    }

    @Test
    void shouldReturnSameRoleWhenAlreadyJastiper() {
        UserProfile user = user(5L, "jas", "jas@example.com", UserRole.JASTIPER.name(), KycStatus.PENDING.name());
        when(repository.findById(5L)).thenReturn(Optional.of(user));

        RoleUpgradeResponse response = profileService.upgradeRoleToJastiper(5L);

        assertEquals(UserRole.JASTIPER.name(), response.getOldRole());
        assertEquals(UserRole.JASTIPER.name(), response.getNewRole());
        verify(repository, never()).save(any(UserProfile.class));
    }

    @Test
    void shouldRejectRoleUpgradeWhenRoleIsNotTitiper() {
        UserProfile user = user(6L, "admin", "admin@example.com", UserRole.ADMIN.name(), KycStatus.PENDING.name());
        when(repository.findById(6L)).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.upgradeRoleToJastiper(6L));

        assertEquals("Hanya user TITIPER yang dapat di-upgrade ke JASTIPER!", ex.getMessage());
    }

    @Test
    void shouldUpgradeRoleFromTitiperToJastiper() {
        UserProfile user = user(7L, "titiper", "titiper@example.com", UserRole.TITIPER.name(), KycStatus.PENDING.name());
        when(repository.findById(7L)).thenReturn(Optional.of(user));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleUpgradeResponse response = profileService.upgradeRoleToJastiper(7L);

        assertEquals(UserRole.TITIPER.name(), response.getOldRole());
        assertEquals(UserRole.JASTIPER.name(), response.getNewRole());
    }

    @Test
    void shouldRejectUpdateProfileWhenRequestNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.updateMyProfile("alice", null));

        assertEquals("Request tidak boleh kosong!", ex.getMessage());
    }

    @Test
    void shouldRejectUpdateProfileWhenUsernameAlreadyUsed() {
        UserProfile current = user(8L, "alice", "alice@example.com", UserRole.TITIPER.name(), KycStatus.PENDING.name());
        when(repository.findByUsername("alice")).thenReturn(Optional.of(current));
        when(repository.findByUsername("taken")).thenReturn(Optional.of(new UserProfile()));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("taken");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.updateMyProfile("alice", request));

        assertEquals("Username sudah terdaftar!", ex.getMessage());
    }

    @Test
    void shouldUpdateProfileFieldsWithTrim() {
        UserProfile current = user(9L, "alice", "alice@example.com", UserRole.TITIPER.name(), KycStatus.PENDING.name());
        when(repository.findByUsername("alice")).thenReturn(Optional.of(current));
        when(repository.findByUsername("new_name")).thenReturn(Optional.empty());
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername(" new_name ");
        request.setFullName(" Alice A ");
        request.setPhoneNumber(" 08123 ");
        request.setBio(" hello ");

        UserProfile saved = profileService.updateMyProfile("alice", request);

        assertEquals("new_name", saved.getUsername());
        assertEquals("Alice A", saved.getFullName());
        assertEquals("08123", saved.getPhoneNumber());
        assertEquals("hello", saved.getBio());
    }

    @Test
    void shouldIgnoreBlankUsernameOnProfileUpdate() {
        UserProfile current = user(10L, "alice", "alice@example.com", UserRole.TITIPER.name(), KycStatus.PENDING.name());
        when(repository.findByUsername("alice")).thenReturn(Optional.of(current));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("   ");

        UserProfile saved = profileService.updateMyProfile("alice", request);

        assertEquals("alice", saved.getUsername());
    }

    @Test
    void shouldRejectKycWhenRequestNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.submitKyc("alice", null));

        assertEquals("Request tidak boleh kosong!", ex.getMessage());
    }

    @Test
    void shouldRejectKycWhenAnyRequiredFieldBlank() {
        KycSubmissionRequest request = new KycSubmissionRequest();
        request.setFullName(" ");
        request.setIdentityDocumentUrl("https://doc");
        request.setSocialMediaUrl("https://social");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.submitKyc("alice", request));

        assertEquals("fullName, identityDocumentUrl, dan socialMediaUrl wajib diisi!", ex.getMessage());
    }

    @Test
    void shouldSubmitKycSuccessfully() {
        UserProfile current = user(11L, "alice", "alice@example.com", UserRole.TITIPER.name(), KycStatus.PENDING.name());
        when(repository.findByUsername("alice")).thenReturn(Optional.of(current));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KycSubmissionRequest request = new KycSubmissionRequest();
        request.setFullName(" Alice A ");
        request.setIdentityDocumentUrl(" https://doc ");
        request.setSocialMediaUrl(" https://social ");

        UserProfile saved = profileService.submitKyc("alice", request);

        assertEquals("Alice A", saved.getFullName());
        assertEquals("https://doc", saved.getKycIdentityDocumentUrl());
        assertEquals("https://social", saved.getKycSocialMediaUrl());
        assertEquals(KycStatus.PENDING.name(), saved.getKycStatus());
    }

    private UserProfile user(Long id, String username, String email, String role, String kycStatus) {
        UserProfile user = new UserProfile();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        user.setKycStatus(kycStatus);
        user.setPassword("encoded");
        return user;
    }
}
