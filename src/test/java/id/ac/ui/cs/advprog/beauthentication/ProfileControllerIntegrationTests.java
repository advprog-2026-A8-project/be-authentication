package id.ac.ui.cs.advprog.beauthentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.beauthentication.model.AccountStatus;
import id.ac.ui.cs.advprog.beauthentication.model.UserRole;
import id.ac.ui.cs.advprog.beauthentication.model.UserProfile;
import id.ac.ui.cs.advprog.beauthentication.repository.UserProfileRepository;
import id.ac.ui.cs.advprog.beauthentication.utils.JwtUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        userProfileRepository.deleteAll();
    }

    private String registerAndLogin(String username, String email, String password) throws Exception {
        Map<String, String> registerRequest = Map.of(
                "username", username,
                "email", email,
                "password", password
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        Map<String, String> loginRequest = Map.of(
                "email", email,
                "password", password
        );

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).path("data").path("token").asText();
    }

    @Test
    void shouldGetMyProfileSuccessfully() throws Exception {
        String token = registerAndLogin("profile_user", "profile_user@example.com", "Password123!");

        mockMvc.perform(get("/api/profile/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profil berhasil diambil!"))
                .andExpect(jsonPath("$.data.username").value("profile_user"))
                .andExpect(jsonPath("$.data.email").value("profile_user@example.com"))
                .andExpect(jsonPath("$.data.role").value("TITIPER"))
                                .andExpect(jsonPath("$.data.kycStatus").value("NOT_SUBMITTED"))
                                .andExpect(jsonPath("$.data.successfulTransactionCount").value(0));
    }

    @Test
    void shouldUpdateMyProfileSuccessfully() throws Exception {
        String token = registerAndLogin("editable_user", "editable_user@example.com", "Password123!");

        Map<String, String> updateRequest = Map.of(
                "username", "updated_user",
                "fullName", "Updated User",
                "phoneNumber", "08123456789",
                "bio", "Bio baru"
        );

        mockMvc.perform(put("/api/profile/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profil berhasil diperbarui!"))
                .andExpect(jsonPath("$.data.username").value("updated_user"))
                .andExpect(jsonPath("$.data.fullName").value("Updated User"))
                .andExpect(jsonPath("$.data.phoneNumber").value("08123456789"))
                .andExpect(jsonPath("$.data.bio").value("Bio baru"));
    }

        @Test
        void shouldSubmitKycSuccessfullyWithBasicData() throws Exception {
        String token = registerAndLogin("kyc_user", "kyc_user@example.com", "Password123!");

        Map<String, String> request = Map.of(
                "fullName", "Budi Santoso",
                "identityDocumentUrl", "https://example.com/ktp-budi.png",
                "socialMediaUrl", "https://instagram.com/budi"
        );

        mockMvc.perform(post("/api/profile/kyc/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Pengajuan KYC berhasil dikirim!"))
                .andExpect(jsonPath("$.data.fullName").value("Budi Santoso"))
                .andExpect(jsonPath("$.data.identityDocumentUrl").value("https://example.com/ktp-budi.png"))
                .andExpect(jsonPath("$.data.socialMediaUrl").value("https://instagram.com/budi"))
                .andExpect(jsonPath("$.data.kycStatus").value("PENDING"));
    }

    @Test
    void shouldRejectKycSubmissionWhenFieldsMissing() throws Exception {
        String token = registerAndLogin("kyc_missing", "kyc_missing@example.com", "Password123!");

        Map<String, String> request = Map.of(
                "fullName", "Budi Santoso"
        );

        mockMvc.perform(post("/api/profile/kyc/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fullName, identityDocumentUrl, dan socialMediaUrl wajib diisi!"));
    }

    @Test
    void shouldRejectUpdateProfileWhenFullNameBlank() throws Exception {
        String token = registerAndLogin("blank_fullname", "blank_fullname@example.com", "Password123!");

        Map<String, String> updateRequest = Map.of(
                "fullName", "   "
        );

        mockMvc.perform(put("/api/profile/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fullName wajib diisi!"));
    }

    @Test
    void shouldRejectUpdateProfileWhenFullNameMissingAndNotSet() throws Exception {
        String token = registerAndLogin("missing_fullname", "missing_fullname@example.com", "Password123!");

        Map<String, String> updateRequest = Map.of(
                "phoneNumber", "08123456789"
        );

        mockMvc.perform(put("/api/profile/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fullName wajib diisi!"));
    }

    @Test
    void shouldAllowUpdateWithoutFullNameWhenAlreadySet() throws Exception {
        String token = registerAndLogin("full_name_set", "full_name_set@example.com", "Password123!");

        Map<String, String> initialUpdate = Map.of(
                "fullName", "Nama Awal"
        );

        mockMvc.perform(put("/api/profile/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initialUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Nama Awal"));

        Map<String, String> secondUpdate = Map.of(
                "phoneNumber", "08000000000"
        );

        mockMvc.perform(put("/api/profile/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Nama Awal"))
                .andExpect(jsonPath("$.data.phoneNumber").value("08000000000"));
    }

    @Test
    void shouldRejectProfileUpdateWhenUsernameAlreadyUsed() throws Exception {
        registerAndLogin("first_user", "first_user@example.com", "Password123!");
        String secondToken = registerAndLogin("second_user", "second_user@example.com", "Password123!");

        Map<String, String> updateRequest = Map.of(
                "username", "first_user",
                "fullName", "Second User"
        );

        mockMvc.perform(put("/api/profile/me")
                        .header("Authorization", "Bearer " + secondToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username sudah terdaftar!"));
    }

    @Test
    void shouldKeepUsernameWhenBlankUsernameProvidedInUpdate() throws Exception {
        String token = registerAndLogin("stable_user", "stable_user@example.com", "Password123!");

        Map<String, String> updateRequest = Map.of(
                "username", "   ",
                "fullName", "Stable Name"
        );

        mockMvc.perform(put("/api/profile/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("stable_user"))
                .andExpect(jsonPath("$.data.fullName").value("Stable Name"));
    }

    @Test
    void shouldStillAccessProfileWithSameTokenAfterUsernameChange() throws Exception {
        String token = registerAndLogin("token_stable_user", "token_stable_user@example.com", "Password123!");

        Map<String, String> updateRequest = Map.of(
                "username", "token_stable_user_updated",
                "fullName", "Token Stable User"
        );

        mockMvc.perform(put("/api/profile/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("token_stable_user_updated"))
                .andExpect(jsonPath("$.data.fullName").value("Token Stable User"));

        mockMvc.perform(get("/api/profile/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("token_stable_user_updated"));
    }

    @Test
    void shouldRejectGetMyProfileWithoutToken() throws Exception {
        mockMvc.perform(get("/api/profile/me"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void shouldLookupProfileByEmailSuccessfully() throws Exception {
        String token = registerAndLogin("lookup_user", "lookup_user@example.com", "Password123!");

        mockMvc.perform(get("/api/profile/lookup")
                        .header("Authorization", "Bearer " + token)
                        .param("email", "lookup_user@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profil berhasil ditemukan!"))
                .andExpect(jsonPath("$.data.username").value("lookup_user"))
                .andExpect(jsonPath("$.data.email").doesNotExist());
    }

    @Test
    void shouldLookupProfileByIdSuccessfully() throws Exception {
        String token = registerAndLogin("lookup_id_user", "lookup_id_user@example.com", "Password123!");

        UUID userId = userProfileRepository.findByEmail("lookup_id_user@example.com")
                .orElseThrow(() -> new AssertionError("User lookup_id_user harus ada")).getId();

        mockMvc.perform(get("/api/profile/lookup")
                        .header("Authorization", "Bearer " + token)
                        .param("id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profil berhasil ditemukan!"))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").doesNotExist());
    }

    @Test
    void shouldLookupProfileByUsernameSuccessfully() throws Exception {
        String token = registerAndLogin("lookup_username_user", "lookup_username_user@example.com", "Password123!");

        mockMvc.perform(get("/api/profile/lookup")
                        .header("Authorization", "Bearer " + token)
                        .param("username", "lookup_username_user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profil berhasil ditemukan!"))
                .andExpect(jsonPath("$.data.username").value("lookup_username_user"))
                .andExpect(jsonPath("$.data.email").doesNotExist());
    }

    @Test
    void shouldRejectLookupProfileWithoutIdentifier() throws Exception {
        String token = registerAndLogin("lookup_req_user", "lookup_req_user@example.com", "Password123!");

        mockMvc.perform(get("/api/profile/lookup")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Salah satu identifier id, username, atau email wajib diisi!"));
    }

        @Test
        void shouldRejectLookupProfileWhenMultipleIdentifiersProvided() throws Exception {
                String token = registerAndLogin("lookup_multi_user", "lookup_multi_user@example.com", "Password123!");

                mockMvc.perform(get("/api/profile/lookup")
                                                .header("Authorization", "Bearer " + token)
                                                .param("username", "lookup_multi_user")
                                                .param("email", "lookup_multi_user@example.com"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Gunakan tepat satu identifier: id, username, atau email."));
        }

    @Test
    void shouldReturnNotFoundWhenLookupProfileDoesNotExist() throws Exception {
        String token = registerAndLogin("lookup_nf_user", "lookup_nf_user@example.com", "Password123!");

        mockMvc.perform(get("/api/profile/lookup")
                        .header("Authorization", "Bearer " + token)
                        .param("username", "pengguna_tidak_ada"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pengguna tidak ditemukan!"));
    }

        @Test
        void shouldAllowLookupProfileWithoutToken() throws Exception {
                UserProfile user = new UserProfile();
                user.setUsername("public_lookup");
                user.setEmail("public_lookup_profile@example.com");
                user.setPassword("dummy");
                user.setRole(UserRole.TITIPER.name());
                user.setKycStatus("PENDING");
                userProfileRepository.save(user);

                mockMvc.perform(get("/api/profile/lookup")
                                                .param("email", "public_lookup_profile@example.com"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Profil berhasil ditemukan!"))
                                .andExpect(jsonPath("$.data.username").value("public_lookup"))
                                .andExpect(jsonPath("$.data.successfulTransactionCount").value(0));
        }

    @Test
    void shouldBulkLookupProfilesSuccessfully() throws Exception {
        String token = registerAndLogin("bulk_one", "bulk_one@example.com", "Password123!");
        registerAndLogin("bulk_two", "bulk_two@example.com", "Password123!");

        UUID firstId = userProfileRepository.findByEmail("bulk_one@example.com")
                .orElseThrow(() -> new AssertionError("User bulk_one harus ada")).getId();
        UUID secondId = userProfileRepository.findByEmail("bulk_two@example.com")
                .orElseThrow(() -> new AssertionError("User bulk_two harus ada")).getId();
        UUID nonExistentId = UUID.randomUUID();

        Map<String, Object> request = Map.of(
                "userIds", List.of(firstId, nonExistentId, secondId)
        );

        mockMvc.perform(post("/api/profile/lookup/bulk")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Bulk lookup profil berhasil!"))
                .andExpect(jsonPath("$.data.users.length()").value(2))
                .andExpect(jsonPath("$.data.users[0].id").value(firstId.toString()))
                .andExpect(jsonPath("$.data.users[0].role").value("TITIPER"))
                .andExpect(jsonPath("$.data.users[1].id").value(secondId.toString()))
                .andExpect(jsonPath("$.data.notFoundIds.length()").value(1))
                .andExpect(jsonPath("$.data.notFoundIds[0]").value(nonExistentId.toString()));
    }

    @Test
    void shouldRejectBulkLookupWhenUserIdsEmpty() throws Exception {
        String token = registerAndLogin("bulk_empty", "bulk_empty@example.com", "Password123!");

        Map<String, Object> request = Map.of(
                "userIds", List.of()
        );

        mockMvc.perform(post("/api/profile/lookup/bulk")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("userIds wajib diisi!"));
    }

        @Test
        void shouldRejectBulkLookupWhenUserIdsContainsNull() throws Exception {
                String token = registerAndLogin("bulk_null", "bulk_null@example.com", "Password123!");

                mockMvc.perform(post("/api/profile/lookup/bulk")
                                                .header("Authorization", "Bearer " + token)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"userIds\":[\"" + UUID.randomUUID() + "\",null,\"" + UUID.randomUUID() + "\"]}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("userIds tidak boleh berisi null!"));
        }

    @Test
    void shouldRejectBulkLookupWithoutToken() throws Exception {
        Map<String, Object> request = Map.of(
                "userIds", List.of(1L, 2L)
        );

        mockMvc.perform(post("/api/profile/lookup/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void shouldAllowAdminToUpgradeRoleToJastiper() throws Exception {
        registerAndLogin("upgrade_target", "upgrade_target@example.com", "Password123!");
        UserProfile targetUser = userProfileRepository.findByEmail("upgrade_target@example.com")
                .orElseThrow(() -> new AssertionError("User target harus ada"));
        UUID targetUserId = targetUser.getId();

        targetUser.setKycStatus("APPROVED");
        userProfileRepository.save(targetUser);

        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");

        Map<String, Object> request = Map.of("userId", targetUserId);

        mockMvc.perform(put("/api/profile/admin/role/upgrade")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role user berhasil di-upgrade ke JASTIPER!"))
                .andExpect(jsonPath("$.data.userId").value(targetUserId.toString()))
                .andExpect(jsonPath("$.data.oldRole").value("TITIPER"))
                .andExpect(jsonPath("$.data.newRole").value("JASTIPER"));

        UserProfile updated = userProfileRepository.findById(targetUserId)
                .orElseThrow(() -> new AssertionError("User target harus tetap ada"));

        Assertions.assertEquals(UserRole.JASTIPER.name(), updated.getRole());
    }

    @Test
    void shouldAllowAdminToApproveKycAndUpgradeRole() throws Exception {
        UserProfile target = new UserProfile();
        target.setUsername("kyc_approve_target");
        target.setEmail("kyc_approve_target@example.com");
        target.setPassword("dummy");
        target.setRole(UserRole.TITIPER.name());
        target.setKycStatus("PENDING");
        UserProfile saved = userProfileRepository.save(target);

        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
        Map<String, Object> request = Map.of(
                "userId", saved.getId(),
                "decision", "APPROVE"
        );

        mockMvc.perform(put("/api/profile/admin/kyc/decision")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Keputusan KYC berhasil diproses!"))
                .andExpect(jsonPath("$.data.userId").value(saved.getId().toString()))
                .andExpect(jsonPath("$.data.oldKycStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.newKycStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.oldRole").value("TITIPER"))
                .andExpect(jsonPath("$.data.newRole").value("JASTIPER"));

        UserProfile updated = userProfileRepository.findById(saved.getId())
                .orElseThrow(() -> new AssertionError("User target harus tetap ada"));
        Assertions.assertEquals("APPROVED", updated.getKycStatus());
        Assertions.assertEquals(UserRole.JASTIPER.name(), updated.getRole());
    }

    @Test
    void shouldAllowAdminToRejectKycWithoutRoleUpgrade() throws Exception {
        UserProfile target = new UserProfile();
        target.setUsername("kyc_reject_target");
        target.setEmail("kyc_reject_target@example.com");
        target.setPassword("dummy");
        target.setRole(UserRole.TITIPER.name());
        target.setKycStatus("PENDING");
        UserProfile saved = userProfileRepository.save(target);

        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
        Map<String, Object> request = Map.of(
                "userId", saved.getId(),
                "decision", "REJECT"
        );

        mockMvc.perform(put("/api/profile/admin/kyc/decision")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Keputusan KYC berhasil diproses!"))
                .andExpect(jsonPath("$.data.userId").value(saved.getId().toString()))
                .andExpect(jsonPath("$.data.oldKycStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.newKycStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.oldRole").value("TITIPER"))
                .andExpect(jsonPath("$.data.newRole").value("TITIPER"));

        UserProfile updated = userProfileRepository.findById(saved.getId())
                .orElseThrow(() -> new AssertionError("User target harus tetap ada"));
        Assertions.assertEquals("REJECTED", updated.getKycStatus());
        Assertions.assertEquals(UserRole.TITIPER.name(), updated.getRole());
    }

    @Test
    void shouldAllowAdminToDemoteJastiperRole() throws Exception {
        UserProfile target = new UserProfile();
        target.setUsername("demote_target");
        target.setEmail("demote_target@example.com");
        target.setPassword("dummy");
        target.setRole(UserRole.JASTIPER.name());
        target.setKycStatus("APPROVED");
        UserProfile saved = userProfileRepository.save(target);

        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
        Map<String, Object> request = Map.of("userId", saved.getId());

        mockMvc.perform(put("/api/profile/admin/role/demote")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role user berhasil di-demote ke TITIPER!"))
                .andExpect(jsonPath("$.data.userId").value(saved.getId().toString()))
                .andExpect(jsonPath("$.data.oldRole").value("JASTIPER"))
                .andExpect(jsonPath("$.data.newRole").value("TITIPER"));

        UserProfile updated = userProfileRepository.findById(saved.getId())
                .orElseThrow(() -> new AssertionError("User target harus tetap ada"));
        Assertions.assertEquals(UserRole.TITIPER.name(), updated.getRole());
    }

        @Test
        void shouldAllowAdminToDemoteTitiperWithoutChanges() throws Exception {
                UserProfile target = new UserProfile();
                target.setUsername("demote_titiper_target");
                target.setEmail("demote_titiper_target@example.com");
                target.setPassword("dummy");
                target.setRole(UserRole.TITIPER.name());
                target.setKycStatus("PENDING");
                UserProfile saved = userProfileRepository.save(target);

                String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
                Map<String, Object> request = Map.of("userId", saved.getId());

                mockMvc.perform(put("/api/profile/admin/role/demote")
                                                .header("Authorization", "Bearer " + adminToken)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Role user berhasil di-demote ke TITIPER!"))
                                .andExpect(jsonPath("$.data.userId").value(saved.getId().toString()))
                                .andExpect(jsonPath("$.data.oldRole").value("TITIPER"))
                                .andExpect(jsonPath("$.data.newRole").value("TITIPER"));
        }

        @Test
        void shouldRejectDemoteRoleWhenTargetIsAdmin() throws Exception {
                UserProfile target = new UserProfile();
                target.setUsername("demote_admin_target");
                target.setEmail("demote_admin_target@example.com");
                target.setPassword("dummy");
                target.setRole(UserRole.ADMIN.name());
                target.setKycStatus("APPROVED");
                UserProfile saved = userProfileRepository.save(target);

                String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
                Map<String, Object> request = Map.of("userId", saved.getId());

                mockMvc.perform(put("/api/profile/admin/role/demote")
                                                .header("Authorization", "Bearer " + adminToken)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Admin tidak dapat di-demote!"));
        }

        @Test
        void shouldRejectDemoteRoleWhenUserIdMissing() throws Exception {
                String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");

                mockMvc.perform(put("/api/profile/admin/role/demote")
                                                .header("Authorization", "Bearer " + adminToken)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("userId wajib diisi!"));
        }

        @Test
        void shouldReturnNotFoundWhenDemoteRoleUserDoesNotExist() throws Exception {
                String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
                Map<String, Object> request = Map.of("userId", UUID.randomUUID());

                mockMvc.perform(put("/api/profile/admin/role/demote")
                                                .header("Authorization", "Bearer " + adminToken)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Pengguna tidak ditemukan!"));
        }

    @Test
    void shouldRejectKycDecisionWhenDecisionMissing() throws Exception {
        UserProfile target = new UserProfile();
        target.setUsername("kyc_missing_decision");
        target.setEmail("kyc_missing_decision@example.com");
        target.setPassword("dummy");
        target.setRole(UserRole.TITIPER.name());
        target.setKycStatus("PENDING");
        UserProfile saved = userProfileRepository.save(target);

        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
        Map<String, Object> request = Map.of("userId", saved.getId());

        mockMvc.perform(put("/api/profile/admin/kyc/decision")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("decision wajib diisi!"));
    }

        @Test
        void shouldRejectKycDecisionWhenUserIdMissing() throws Exception {
                String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
                Map<String, Object> request = Map.of("decision", "APPROVE");

                mockMvc.perform(put("/api/profile/admin/kyc/decision")
                                                .header("Authorization", "Bearer " + adminToken)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("userId wajib diisi!"));
        }

    @Test
    void shouldRejectKycDecisionWhenDecisionInvalid() throws Exception {
        UserProfile target = new UserProfile();
        target.setUsername("kyc_invalid_decision");
        target.setEmail("kyc_invalid_decision@example.com");
        target.setPassword("dummy");
        target.setRole(UserRole.TITIPER.name());
        target.setKycStatus("PENDING");
        UserProfile saved = userProfileRepository.save(target);

        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
        Map<String, Object> request = Map.of(
                "userId", saved.getId(),
                "decision", "INVALID"
        );

        mockMvc.perform(put("/api/profile/admin/kyc/decision")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("decision tidak valid!"));
    }

    @Test
    void shouldReturnNotFoundWhenKycDecisionUserDoesNotExist() throws Exception {
        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
        Map<String, Object> request = Map.of(
                "userId", UUID.randomUUID(),
                "decision", "APPROVE"
        );

        mockMvc.perform(put("/api/profile/admin/kyc/decision")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pengguna tidak ditemukan!"));
    }

    @Test
    void shouldRejectKycDecisionForNonAdmin() throws Exception {
        UserProfile target = new UserProfile();
        target.setUsername("kyc_non_admin_target");
        target.setEmail("kyc_non_admin_target@example.com");
        target.setPassword("dummy");
        target.setRole(UserRole.TITIPER.name());
        target.setKycStatus("PENDING");
        UserProfile saved = userProfileRepository.save(target);

        String nonAdminToken = jwtUtil.generateToken("titiper_test@example.com", "TITIPER");
        Map<String, Object> request = Map.of(
                "userId", saved.getId(),
                "decision", "APPROVE"
        );

        mockMvc.perform(put("/api/profile/admin/kyc/decision")
                        .header("Authorization", "Bearer " + nonAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void shouldRejectKycDecisionWithoutToken() throws Exception {
        Map<String, Object> request = Map.of(
                "userId", 1L,
                "decision", "APPROVE"
        );

        mockMvc.perform(put("/api/profile/admin/kyc/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void shouldRejectDemoteRoleForNonAdmin() throws Exception {
        UserProfile target = new UserProfile();
        target.setUsername("demote_non_admin_target");
        target.setEmail("demote_non_admin_target@example.com");
        target.setPassword("dummy");
        target.setRole(UserRole.JASTIPER.name());
        target.setKycStatus("APPROVED");
        UserProfile saved = userProfileRepository.save(target);

        String nonAdminToken = jwtUtil.generateToken("titiper_test@example.com", "TITIPER");
        Map<String, Object> request = Map.of("userId", saved.getId());

        mockMvc.perform(put("/api/profile/admin/role/demote")
                        .header("Authorization", "Bearer " + nonAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

        @Test
        void shouldRejectDemoteRoleWithoutToken() throws Exception {
                Map<String, Object> request = Map.of("userId", 1L);

                mockMvc.perform(put("/api/profile/admin/role/demote")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
        }

            @Test
            void shouldAllowAdminToIncrementJastiperStats() throws Exception {
                UserProfile target = new UserProfile();
                target.setUsername("stats_jastiper");
                target.setEmail("stats_jastiper@example.com");
                target.setPassword("dummy");
                target.setRole(UserRole.JASTIPER.name());
                target.setKycStatus("APPROVED");
                target.setSuccessfulTransactionCount(2L);
                UserProfile saved = userProfileRepository.save(target);

                String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
                Map<String, Object> request = Map.of(
                        "userId", saved.getId(),
                        "delta", 3
                );

                mockMvc.perform(put("/api/profile/admin/jastiper/stats")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value("Statistik Jastiper berhasil diperbarui!"))
                        .andExpect(jsonPath("$.data.userId").value(saved.getId().toString()))
                        .andExpect(jsonPath("$.data.oldCount").value(2))
                        .andExpect(jsonPath("$.data.newCount").value(5));

                UserProfile updated = userProfileRepository.findById(saved.getId())
                        .orElseThrow(() -> new AssertionError("User target harus tetap ada"));
                Assertions.assertEquals(5L, updated.getSuccessfulTransactionCount());
            }

            @Test
            void shouldRejectJastiperStatsUpdateWhenUserIdMissing() throws Exception {
                String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
                Map<String, Object> request = Map.of("delta", 1);

                mockMvc.perform(put("/api/profile/admin/jastiper/stats")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("userId wajib diisi!"));
            }

            @Test
            void shouldRejectJastiperStatsUpdateWhenDeltaMissing() throws Exception {
                UserProfile target = new UserProfile();
                target.setUsername("stats_missing_delta");
                target.setEmail("stats_missing_delta@example.com");
                target.setPassword("dummy");
                target.setRole(UserRole.JASTIPER.name());
                target.setKycStatus("APPROVED");
                UserProfile saved = userProfileRepository.save(target);

                String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
                Map<String, Object> request = Map.of("userId", saved.getId());

                mockMvc.perform(put("/api/profile/admin/jastiper/stats")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("delta wajib diisi!"));
            }

            @Test
            void shouldRejectJastiperStatsUpdateWhenDeltaNotPositive() throws Exception {
                UserProfile target = new UserProfile();
                target.setUsername("stats_delta_zero");
                target.setEmail("stats_delta_zero@example.com");
                target.setPassword("dummy");
                target.setRole(UserRole.JASTIPER.name());
                target.setKycStatus("APPROVED");
                UserProfile saved = userProfileRepository.save(target);

                String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
                Map<String, Object> request = Map.of(
                        "userId", saved.getId(),
                        "delta", 0
                );

                mockMvc.perform(put("/api/profile/admin/jastiper/stats")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("delta harus lebih besar dari 0!"));
            }

            @Test
            void shouldRejectJastiperStatsUpdateForNonJastiper() throws Exception {
                UserProfile target = new UserProfile();
                target.setUsername("stats_non_jastiper");
                target.setEmail("stats_non_jastiper@example.com");
                target.setPassword("dummy");
                target.setRole(UserRole.TITIPER.name());
                target.setKycStatus("PENDING");
                UserProfile saved = userProfileRepository.save(target);

                String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
                Map<String, Object> request = Map.of(
                        "userId", saved.getId(),
                        "delta", 1
                );

                mockMvc.perform(put("/api/profile/admin/jastiper/stats")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("Hanya JASTIPER yang dapat diupdate statistiknya!"));
            }

            @Test
            void shouldRejectJastiperStatsUpdateWhenUserNotFound() throws Exception {
                String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
                Map<String, Object> request = Map.of(
                        "userId", UUID.randomUUID(),
                        "delta", 1
                );

                mockMvc.perform(put("/api/profile/admin/jastiper/stats")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.message").value("Pengguna tidak ditemukan!"));
            }

            @Test
            void shouldRejectJastiperStatsUpdateForNonAdmin() throws Exception {
                UserProfile target = new UserProfile();
                target.setUsername("stats_non_admin");
                target.setEmail("stats_non_admin@example.com");
                target.setPassword("dummy");
                target.setRole(UserRole.JASTIPER.name());
                target.setKycStatus("APPROVED");
                UserProfile saved = userProfileRepository.save(target);

                String nonAdminToken = jwtUtil.generateToken("titiper_test@example.com", "TITIPER");
                Map<String, Object> request = Map.of(
                        "userId", saved.getId(),
                        "delta", 1
                );

                mockMvc.perform(put("/api/profile/admin/jastiper/stats")
                                .header("Authorization", "Bearer " + nonAdminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.message").value("Akses ditolak!"));
            }

            @Test
            void shouldRejectJastiperStatsUpdateWithoutToken() throws Exception {
                Map<String, Object> request = Map.of(
                        "userId", 1L,
                        "delta", 1
                );

                mockMvc.perform(put("/api/profile/admin/jastiper/stats")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
            }

    @Test
    void shouldRejectRoleUpgradeWhenUserIdMissing() throws Exception {
        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");

        mockMvc.perform(put("/api/profile/admin/role/upgrade")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("userId wajib diisi!"));
    }

    @Test
    void shouldReturnNotFoundWhenRoleUpgradeUserDoesNotExist() throws Exception {
        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");

        Map<String, Object> request = Map.of("userId", UUID.randomUUID());

        mockMvc.perform(put("/api/profile/admin/role/upgrade")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pengguna tidak ditemukan!"));
    }

    @Test
    void shouldRejectRoleUpgradeWhenTargetIsNotTitiper() throws Exception {
        UserProfile adminTarget = new UserProfile();
        adminTarget.setUsername("already_admin_target");
        adminTarget.setEmail("already_admin_target@example.com");
        adminTarget.setPassword("dummy");
        adminTarget.setRole(UserRole.ADMIN.name());
        adminTarget.setKycStatus("APPROVED");

        UserProfile saved = userProfileRepository.save(adminTarget);
        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");

        Map<String, Object> request = Map.of("userId", saved.getId());

        mockMvc.perform(put("/api/profile/admin/role/upgrade")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Hanya user TITIPER yang dapat di-upgrade ke JASTIPER!"));
    }

    @Test
    void shouldRejectRoleUpgradeForNonAdmin() throws Exception {
        registerAndLogin("upgrade_non_admin", "upgrade_non_admin@example.com", "Password123!");
        UUID targetUserId = userProfileRepository.findByEmail("upgrade_non_admin@example.com")
                .orElseThrow(() -> new AssertionError("User target harus ada")).getId();

        String nonAdminToken = jwtUtil.generateToken("titiper_test@example.com", "TITIPER");

        Map<String, Object> request = Map.of("userId", targetUserId);

        mockMvc.perform(put("/api/profile/admin/role/upgrade")
                        .header("Authorization", "Bearer " + nonAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void shouldRejectRoleUpgradeWithoutToken() throws Exception {
        Map<String, Object> request = Map.of("userId", 1L);

        mockMvc.perform(put("/api/profile/admin/role/upgrade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void shouldAllowAdminToGetAllUsers() throws Exception {
        UserProfile userOne = new UserProfile();
        userOne.setUsername("admin_list_one");
        userOne.setEmail("admin_list_one@example.com");
        userOne.setPassword("dummy");
        userOne.setRole(UserRole.TITIPER.name());
        userOne.setAccountStatus(AccountStatus.ACTIVE.name());
        userOne.setKycStatus("PENDING");

        UserProfile userTwo = new UserProfile();
        userTwo.setUsername("admin_list_two");
        userTwo.setEmail("admin_list_two@example.com");
        userTwo.setPassword("dummy");
        userTwo.setRole(UserRole.JASTIPER.name());
        userTwo.setAccountStatus(AccountStatus.ACTIVE.name());
        userTwo.setKycStatus("APPROVED");

        userProfileRepository.saveAll(List.of(userOne, userTwo));

        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");

        mockMvc.perform(get("/api/profile/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Daftar profil berhasil diambil!"))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void shouldRejectAdminUsersForNonAdmin() throws Exception {
        String token = jwtUtil.generateToken("titiper_test@example.com", "TITIPER");

        mockMvc.perform(get("/api/profile/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void shouldRejectAdminUsersWithoutToken() throws Exception {
        mockMvc.perform(get("/api/profile/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void shouldAllowAdminToUpdateAccountStatus() throws Exception {
        UserProfile target = new UserProfile();
        target.setUsername("status_target");
        target.setEmail("status_target@example.com");
        target.setPassword("dummy");
        target.setRole(UserRole.TITIPER.name());
        target.setAccountStatus(AccountStatus.ACTIVE.name());
        target.setKycStatus("PENDING");
        UserProfile saved = userProfileRepository.save(target);

        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
        Map<String, Object> request = Map.of(
                "userId", saved.getId(),
                "status", "BANNED"
        );

        mockMvc.perform(put("/api/profile/admin/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Status akun berhasil diperbarui!"))
                .andExpect(jsonPath("$.data.userId").value(saved.getId().toString()))
                .andExpect(jsonPath("$.data.oldStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.newStatus").value("BANNED"));

        UserProfile updated = userProfileRepository.findById(saved.getId())
                .orElseThrow(() -> new AssertionError("User target harus tetap ada"));
        Assertions.assertEquals(AccountStatus.BANNED.name(), updated.getAccountStatus());
    }

    @Test
    void shouldRejectAccountStatusUpdateWithInvalidStatus() throws Exception {
        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");

        Map<String, Object> request = Map.of(
                "userId", UUID.randomUUID(),
                "status", "PENDING"
        );

        mockMvc.perform(put("/api/profile/admin/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("status tidak valid!"));
    }

    @Test
    void shouldRejectAccountStatusUpdateWhenUserIdMissing() throws Exception {
        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");

        mockMvc.perform(put("/api/profile/admin/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("userId wajib diisi!"));
    }

    @Test
    void shouldRejectAccountStatusUpdateForNonAdmin() throws Exception {
        String token = jwtUtil.generateToken("titiper_test@example.com", "TITIPER");

        Map<String, Object> request = Map.of(
                "userId", UUID.randomUUID(),
                "status", "BANNED"
        );

        mockMvc.perform(put("/api/profile/admin/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void shouldRejectAccountStatusUpdateWithoutToken() throws Exception {
        Map<String, Object> request = Map.of(
                "userId", 1L,
                "status", "BANNED"
        );

        mockMvc.perform(put("/api/profile/admin/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

        @Test
        void shouldAllowGetJastiperProfilesWithoutToken() throws Exception {
                UserProfile jastiper = new UserProfile();
                jastiper.setUsername("jastiper_public_list");
                jastiper.setEmail("jastiper_public_list@example.com");
                jastiper.setPassword("dummy");
                jastiper.setRole(UserRole.JASTIPER.name());
                jastiper.setKycStatus("APPROVED");
                jastiper.setSuccessfulTransactionCount(4L);
                userProfileRepository.save(jastiper);

                mockMvc.perform(get("/api/profile/jastiper"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Daftar jastiper berhasil diambil!"))
                                .andExpect(jsonPath("$.data[0].username").value("jastiper_public_list"))
                                .andExpect(jsonPath("$.data[0].successfulTransactionCount").value(4));
        }

    @Test
    void shouldGetOnlyJastiperProfiles() throws Exception {
        String token = registerAndLogin("viewer_user", "viewer_user@example.com", "Password123!");

        UserProfile jastiper = new UserProfile();
        jastiper.setUsername("jastiper_one");
        jastiper.setEmail("jastiper_one@example.com");
        jastiper.setPassword("dummy");
        jastiper.setRole(UserRole.JASTIPER.name());
        jastiper.setKycStatus("PENDING");
                jastiper.setSuccessfulTransactionCount(7L);

        UserProfile nonJastiper = new UserProfile();
        nonJastiper.setUsername("titiper_one");
        nonJastiper.setEmail("titiper_one@example.com");
        nonJastiper.setPassword("dummy");
        nonJastiper.setRole(UserRole.TITIPER.name());
        nonJastiper.setKycStatus("PENDING");

        userProfileRepository.saveAll(List.of(jastiper, nonJastiper));

        mockMvc.perform(get("/api/profile/jastiper")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Daftar jastiper berhasil diambil!"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].username").value("jastiper_one"))
                .andExpect(jsonPath("$.data[0].role").value("JASTIPER"))
                .andExpect(jsonPath("$.data[0].successfulTransactionCount").value(7));
    }

    @Test
    void shouldRejectUpdateMyProfileWithoutToken() throws Exception {
        Map<String, String> updateRequest = Map.of(
                "fullName", "No Token User"
        );

        mockMvc.perform(put("/api/profile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void shouldSubmitKycSuccessfully() throws Exception {
        String token = registerAndLogin("kyc_user", "kyc_user@example.com", "Password123!");

        Map<String, String> kycRequest = Map.of(
                "fullName", "KYC User",
                "identityDocumentUrl", "https://example.com/identity-doc",
                "socialMediaUrl", "https://instagram.com/kyc_user"
        );

        mockMvc.perform(post("/api/profile/kyc/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(kycRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Pengajuan KYC berhasil dikirim!"))
                .andExpect(jsonPath("$.data.fullName").value("KYC User"))
                .andExpect(jsonPath("$.data.identityDocumentUrl").value("https://example.com/identity-doc"))
                .andExpect(jsonPath("$.data.socialMediaUrl").value("https://instagram.com/kyc_user"))
                .andExpect(jsonPath("$.data.kycStatus").value("PENDING"));
    }

    @Test
    void shouldRejectKycSubmitWithInvalidPayload() throws Exception {
        String token = registerAndLogin("kyc_invalid_user", "kyc_invalid_user@example.com", "Password123!");

        Map<String, String> invalidKycRequest = Map.of(
                "fullName", "",
                "identityDocumentUrl", "https://example.com/identity-doc",
                "socialMediaUrl", ""
        );

        mockMvc.perform(post("/api/profile/kyc/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidKycRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fullName, identityDocumentUrl, dan socialMediaUrl wajib diisi!"));
    }

    @Test
    void shouldRejectKycSubmitWithoutToken() throws Exception {
        Map<String, String> kycRequest = Map.of(
                "fullName", "No Token",
                "identityDocumentUrl", "https://example.com/identity-doc",
                "socialMediaUrl", "https://instagram.com/no_token"
        );

        mockMvc.perform(post("/api/profile/kyc/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(kycRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void shouldNotExposeKycDocumentUrlsInProfileList() throws Exception {
        String token = registerAndLogin("list_user", "list_user@example.com", "Password123!");

        Map<String, String> kycRequest = Map.of(
                "fullName", "List User",
                "identityDocumentUrl", "https://example.com/private-doc",
                "socialMediaUrl", "https://instagram.com/list_user"
        );

        mockMvc.perform(post("/api/profile/kyc/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(kycRequest)))
                .andExpect(status().isOk());

        String adminToken = jwtUtil.generateToken("list_user@example.com", "ADMIN");
        mockMvc.perform(get("/api/profile/all")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Daftar profil berhasil diambil!"))
                .andExpect(jsonPath("$.data[0].username").value("list_user"))
                .andExpect(jsonPath("$.data[0].kycStatus").value("PENDING"))
                .andExpect(jsonPath("$.data[0].kycIdentityDocumentUrl").doesNotExist())
                .andExpect(jsonPath("$.data[0].kycSocialMediaUrl").doesNotExist());
    }

    @Test
    void shouldCompleteMilestone50FlowEndToEnd() throws Exception {
        Map<String, String> registerRequest = Map.of(
                "email", "milestone50@example.com",
                "password", "Password123!"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("TITIPER"))
                .andExpect(jsonPath("$.data.kycStatus").value("NOT_SUBMITTED"))
                .andExpect(jsonPath("$.data.username").isString())
                .andReturn();

        String generatedUsername = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .path("data").path("username").asText();

        Map<String, String> loginRequest = Map.of(
                "email", "milestone50@example.com",
                "password", "Password123!"
        );

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("token").asText();

        Map<String, String> updateRequest = Map.of(
                "fullName", "Milestone Fifty",
                "phoneNumber", "08111111111",
                "bio", "Profile updated in milestone 50"
        );

        mockMvc.perform(put("/api/profile/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(generatedUsername))
                .andExpect(jsonPath("$.data.fullName").value("Milestone Fifty"));

        Map<String, String> kycRequest = Map.of(
                "fullName", "Milestone Fifty",
                "identityDocumentUrl", "https://example.com/milestone50-doc",
                "socialMediaUrl", "https://instagram.com/milestone50"
        );

        mockMvc.perform(post("/api/profile/kyc/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(kycRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kycStatus").value("PENDING"));

        mockMvc.perform(get("/api/profile/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(generatedUsername))
                .andExpect(jsonPath("$.data.fullName").value("Milestone Fifty"))
                .andExpect(jsonPath("$.data.phoneNumber").value("08111111111"))
                .andExpect(jsonPath("$.data.kycStatus").value("PENDING"));
    }

    @Test
    void shouldPersistProfileUpdateInDatabase() throws Exception {
        String token = registerAndLogin("db_profile_user", "db_profile_user@example.com", "Password123!");

        Map<String, String> updateRequest = Map.of(
                "fullName", "Persisted Profile",
                "phoneNumber", "08222222222",
                "bio", "Persisted bio"
        );

        mockMvc.perform(put("/api/profile/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        UserProfile savedUser = userProfileRepository.findByEmail("db_profile_user@example.com")
                .orElseThrow(() -> new AssertionError("User seharusnya ada di database"));

        Assertions.assertEquals("Persisted Profile", savedUser.getFullName());
        Assertions.assertEquals("08222222222", savedUser.getPhoneNumber());
        Assertions.assertEquals("Persisted bio", savedUser.getBio());
    }

    @Test
    void shouldPersistKycDataInDatabase() throws Exception {
        String token = registerAndLogin("db_kyc_user", "db_kyc_user@example.com", "Password123!");

        Map<String, String> kycRequest = Map.of(
                "fullName", "Persisted KYC User",
                "identityDocumentUrl", "https://example.com/persisted-doc",
                "socialMediaUrl", "https://instagram.com/persisted_kyc"
        );

        mockMvc.perform(post("/api/profile/kyc/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(kycRequest)))
                .andExpect(status().isOk());

        UserProfile savedUser = userProfileRepository.findByEmail("db_kyc_user@example.com")
                .orElseThrow(() -> new AssertionError("User seharusnya ada di database"));

        Assertions.assertEquals("Persisted KYC User", savedUser.getFullName());
        Assertions.assertEquals("https://example.com/persisted-doc", savedUser.getKycIdentityDocumentUrl());
        Assertions.assertEquals("https://instagram.com/persisted_kyc", savedUser.getKycSocialMediaUrl());
        Assertions.assertEquals("PENDING", savedUser.getKycStatus());
    }

    // =====================================================================
    // New tests: KYC decision guard (Bug 3)
    // =====================================================================

    @Test
    void shouldRejectKycDecisionWhenKycStatusIsNotSubmitted() throws Exception {
        UserProfile target = new UserProfile();
        target.setUsername("kyc_not_submitted");
        target.setEmail("kyc_not_submitted@example.com");
        target.setPassword("dummy");
        target.setRole(UserRole.TITIPER.name());
        target.setKycStatus("NOT_SUBMITTED");
        UserProfile saved = userProfileRepository.save(target);

        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
        Map<String, Object> request = Map.of("userId", saved.getId(), "decision", "APPROVE");

        mockMvc.perform(put("/api/profile/admin/kyc/decision")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("KYC hanya dapat diputuskan jika statusnya PENDING!"));
    }

    @Test
    void shouldRejectKycDecisionWhenKycStatusIsAlreadyApproved() throws Exception {
        UserProfile target = new UserProfile();
        target.setUsername("kyc_already_approved");
        target.setEmail("kyc_already_approved@example.com");
        target.setPassword("dummy");
        target.setRole(UserRole.JASTIPER.name());
        target.setKycStatus("APPROVED");
        UserProfile saved = userProfileRepository.save(target);

        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
        Map<String, Object> request = Map.of("userId", saved.getId(), "decision", "REJECT");

        mockMvc.perform(put("/api/profile/admin/kyc/decision")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("KYC hanya dapat diputuskan jika statusnya PENDING!"));
    }

    @Test
    void shouldRejectKycDecisionWhenKycStatusIsAlreadyRejected() throws Exception {
        UserProfile target = new UserProfile();
        target.setUsername("kyc_already_rejected");
        target.setEmail("kyc_already_rejected@example.com");
        target.setPassword("dummy");
        target.setRole(UserRole.TITIPER.name());
        target.setKycStatus("REJECTED");
        UserProfile saved = userProfileRepository.save(target);

        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
        Map<String, Object> request = Map.of("userId", saved.getId(), "decision", "APPROVE");

        mockMvc.perform(put("/api/profile/admin/kyc/decision")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("KYC hanya dapat diputuskan jika statusnya PENDING!"));
    }

    // =====================================================================
    // New tests: upgradeRoleToJastiper KYC guard (Bug 2)
    // =====================================================================

    @Test
    void shouldRejectRoleUpgradeWhenKycStatusIsNotSubmitted() throws Exception {
        UserProfile target = new UserProfile();
        target.setUsername("upgrade_not_submitted");
        target.setEmail("upgrade_not_submitted@example.com");
        target.setPassword("dummy");
        target.setRole(UserRole.TITIPER.name());
        target.setKycStatus("NOT_SUBMITTED");
        UserProfile saved = userProfileRepository.save(target);

        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
        Map<String, Object> request = Map.of("userId", saved.getId());

        mockMvc.perform(put("/api/profile/admin/role/upgrade")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Hanya user dengan KYC APPROVED yang dapat di-upgrade ke JASTIPER!"));
    }

    @Test
    void shouldRejectRoleUpgradeWhenKycStatusIsPending() throws Exception {
        UserProfile target = new UserProfile();
        target.setUsername("upgrade_pending_kyc");
        target.setEmail("upgrade_pending_kyc@example.com");
        target.setPassword("dummy");
        target.setRole(UserRole.TITIPER.name());
        target.setKycStatus("PENDING");
        UserProfile saved = userProfileRepository.save(target);

        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
        Map<String, Object> request = Map.of("userId", saved.getId());

        mockMvc.perform(put("/api/profile/admin/role/upgrade")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Hanya user dengan KYC APPROVED yang dapat di-upgrade ke JASTIPER!"));
    }

    // =====================================================================
    // New tests: submitKyc accountStatus flow (Bug 4)
    // =====================================================================

    @Test
    void shouldSetAccountStatusToPendingAfterKycSubmission() throws Exception {
        String token = registerAndLogin("kyc_status_check", "kyc_status_check@example.com", "Password123!");

        Map<String, String> kycRequest = Map.of(
                "fullName", "Status Check User",
                "identityDocumentUrl", "https://example.com/status-check-doc",
                "socialMediaUrl", "https://instagram.com/status_check"
        );

        mockMvc.perform(post("/api/profile/kyc/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(kycRequest)))
                .andExpect(status().isOk());

        UserProfile savedUser = userProfileRepository.findByEmail("kyc_status_check@example.com")
                .orElseThrow(() -> new AssertionError("User seharusnya ada di database"));

        Assertions.assertEquals("PENDING", savedUser.getKycStatus());
        Assertions.assertEquals(AccountStatus.PENDING.name(), savedUser.getAccountStatus());
    }

    @Test
    void shouldResetAccountStatusToActiveAfterKycApproval() throws Exception {
        UserProfile target = new UserProfile();
        target.setUsername("kyc_approve_status");
        target.setEmail("kyc_approve_status@example.com");
        target.setPassword("dummy");
        target.setRole(UserRole.TITIPER.name());
        target.setKycStatus("PENDING");
        target.setAccountStatus(AccountStatus.PENDING.name());
        UserProfile saved = userProfileRepository.save(target);

        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
        Map<String, Object> request = Map.of("userId", saved.getId(), "decision", "APPROVE");

        mockMvc.perform(put("/api/profile/admin/kyc/decision")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        UserProfile updated = userProfileRepository.findById(saved.getId())
                .orElseThrow(() -> new AssertionError("User target harus tetap ada"));

        Assertions.assertEquals("APPROVED", updated.getKycStatus());
        Assertions.assertEquals(AccountStatus.ACTIVE.name(), updated.getAccountStatus());
    }

    @Test
    void shouldResetAccountStatusToActiveAfterKycRejection() throws Exception {
        UserProfile target = new UserProfile();
        target.setUsername("kyc_reject_status");
        target.setEmail("kyc_reject_status@example.com");
        target.setPassword("dummy");
        target.setRole(UserRole.TITIPER.name());
        target.setKycStatus("PENDING");
        target.setAccountStatus(AccountStatus.PENDING.name());
        UserProfile saved = userProfileRepository.save(target);

        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");
        Map<String, Object> request = Map.of("userId", saved.getId(), "decision", "REJECT");

        mockMvc.perform(put("/api/profile/admin/kyc/decision")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        UserProfile updated = userProfileRepository.findById(saved.getId())
                .orElseThrow(() -> new AssertionError("User target harus tetap ada"));

        Assertions.assertEquals("REJECTED", updated.getKycStatus());
        Assertions.assertEquals(AccountStatus.ACTIVE.name(), updated.getAccountStatus());
    }

    @Test
    void shouldRejectKycResubmissionWhenAlreadyApproved() throws Exception {
        String token = registerAndLogin("kyc_resubmit", "kyc_resubmit@example.com", "Password123!");

        UserProfile user = userProfileRepository.findByEmail("kyc_resubmit@example.com")
                .orElseThrow(() -> new AssertionError("User harus ada"));
        user.setKycStatus("APPROVED");
        userProfileRepository.save(user);

        Map<String, String> kycRequest = Map.of(
                "fullName", "Resubmit User",
                "identityDocumentUrl", "https://example.com/doc",
                "socialMediaUrl", "https://instagram.com/resubmit"
        );

        mockMvc.perform(post("/api/profile/kyc/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(kycRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("KYC sudah disetujui dan tidak dapat diajukan ulang!"));
    }

    // =====================================================================
    // New tests: public endpoint field safety (Bug 5)
    // =====================================================================

    @Test
    void shouldNotExposeEmailOrAccountStatusOnPublicLookup() throws Exception {
        UserProfile user = new UserProfile();
        user.setUsername("public_safe_lookup");
        user.setEmail("public_safe_lookup@example.com");
        user.setPassword("dummy");
        user.setRole(UserRole.TITIPER.name());
        user.setKycStatus("NOT_SUBMITTED");
        userProfileRepository.save(user);

        mockMvc.perform(get("/api/profile/lookup")
                        .param("username", "public_safe_lookup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("public_safe_lookup"))
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.data.accountStatus").doesNotExist())
                .andExpect(jsonPath("$.data.kycStatus").value("NOT_SUBMITTED"));
    }

    @Test
    void shouldNotExposeEmailOrAccountStatusOnPublicJastiperList() throws Exception {
        UserProfile jastiper = new UserProfile();
        jastiper.setUsername("public_safe_jastiper");
        jastiper.setEmail("public_safe_jastiper@example.com");
        jastiper.setPassword("dummy");
        jastiper.setRole(UserRole.JASTIPER.name());
        jastiper.setKycStatus("APPROVED");
        jastiper.setSuccessfulTransactionCount(3L);
        userProfileRepository.save(jastiper);

        mockMvc.perform(get("/api/profile/jastiper"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].email").doesNotExist())
                .andExpect(jsonPath("$.data[0].phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.data[0].accountStatus").doesNotExist());
    }
}




