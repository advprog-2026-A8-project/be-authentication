package id.ac.ui.cs.advprog.beauthentication;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        String token = registerAndLogin("profile_user", "profile_user@example.com", "password123");

        mockMvc.perform(get("/api/profile/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profil berhasil diambil!"))
                .andExpect(jsonPath("$.data.username").value("profile_user"))
                .andExpect(jsonPath("$.data.email").value("profile_user@example.com"))
                .andExpect(jsonPath("$.data.role").value("TITIPER"))
                .andExpect(jsonPath("$.data.kycStatus").value("PENDING"));
    }

    @Test
    void shouldUpdateMyProfileSuccessfully() throws Exception {
        String token = registerAndLogin("editable_user", "editable_user@example.com", "password123");

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
        String token = registerAndLogin("kyc_user", "kyc_user@example.com", "password123");

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
        String token = registerAndLogin("kyc_missing", "kyc_missing@example.com", "password123");

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
        String token = registerAndLogin("blank_fullname", "blank_fullname@example.com", "password123");

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
        String token = registerAndLogin("missing_fullname", "missing_fullname@example.com", "password123");

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
    void shouldRejectProfileUpdateWhenUsernameAlreadyUsed() throws Exception {
        registerAndLogin("first_user", "first_user@example.com", "password123");
        String secondToken = registerAndLogin("second_user", "second_user@example.com", "password123");

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
        String token = registerAndLogin("stable_user", "stable_user@example.com", "password123");

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
        String token = registerAndLogin("token_stable_user", "token_stable_user@example.com", "password123");

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
        String token = registerAndLogin("lookup_user", "lookup_user@example.com", "password123");

        mockMvc.perform(get("/api/profile/lookup")
                        .header("Authorization", "Bearer " + token)
                        .param("email", "lookup_user@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profil berhasil ditemukan!"))
                .andExpect(jsonPath("$.data.username").value("lookup_user"))
                .andExpect(jsonPath("$.data.email").value("lookup_user@example.com"));
    }

    @Test
    void shouldLookupProfileByIdSuccessfully() throws Exception {
        String token = registerAndLogin("lookup_id_user", "lookup_id_user@example.com", "password123");

        Long userId = userProfileRepository.findByEmail("lookup_id_user@example.com")
                .orElseThrow(() -> new AssertionError("User lookup_id_user harus ada")).getId();

        mockMvc.perform(get("/api/profile/lookup")
                        .header("Authorization", "Bearer " + token)
                        .param("id", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profil berhasil ditemukan!"))
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.email").value("lookup_id_user@example.com"));
    }

    @Test
    void shouldLookupProfileByUsernameSuccessfully() throws Exception {
        String token = registerAndLogin("lookup_username_user", "lookup_username_user@example.com", "password123");

        mockMvc.perform(get("/api/profile/lookup")
                        .header("Authorization", "Bearer " + token)
                        .param("username", "lookup_username_user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profil berhasil ditemukan!"))
                .andExpect(jsonPath("$.data.username").value("lookup_username_user"))
                .andExpect(jsonPath("$.data.email").value("lookup_username_user@example.com"));
    }

    @Test
    void shouldRejectLookupProfileWithoutIdentifier() throws Exception {
        String token = registerAndLogin("lookup_req_user", "lookup_req_user@example.com", "password123");

        mockMvc.perform(get("/api/profile/lookup")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Salah satu identifier id, username, atau email wajib diisi!"));
    }

        @Test
        void shouldRejectLookupProfileWhenMultipleIdentifiersProvided() throws Exception {
                String token = registerAndLogin("lookup_multi_user", "lookup_multi_user@example.com", "password123");

                mockMvc.perform(get("/api/profile/lookup")
                                                .header("Authorization", "Bearer " + token)
                                                .param("username", "lookup_multi_user")
                                                .param("email", "lookup_multi_user@example.com"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Gunakan tepat satu identifier: id, username, atau email."));
        }

    @Test
    void shouldReturnNotFoundWhenLookupProfileDoesNotExist() throws Exception {
        String token = registerAndLogin("lookup_nf_user", "lookup_nf_user@example.com", "password123");

        mockMvc.perform(get("/api/profile/lookup")
                        .header("Authorization", "Bearer " + token)
                        .param("username", "pengguna_tidak_ada"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pengguna tidak ditemukan!"));
    }

    @Test
    void shouldRejectLookupProfileWithoutToken() throws Exception {
        mockMvc.perform(get("/api/profile/lookup")
                        .param("email", "someone@example.com"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void shouldBulkLookupProfilesSuccessfully() throws Exception {
        String token = registerAndLogin("bulk_one", "bulk_one@example.com", "password123");
        registerAndLogin("bulk_two", "bulk_two@example.com", "password123");

        Long firstId = userProfileRepository.findByEmail("bulk_one@example.com")
                .orElseThrow(() -> new AssertionError("User bulk_one harus ada")).getId();
        Long secondId = userProfileRepository.findByEmail("bulk_two@example.com")
                .orElseThrow(() -> new AssertionError("User bulk_two harus ada")).getId();

        Map<String, Object> request = Map.of(
                "userIds", List.of(firstId, 999999L, secondId)
        );

        mockMvc.perform(post("/api/profile/lookup/bulk")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Bulk lookup profil berhasil!"))
                .andExpect(jsonPath("$.data.users.length()").value(2))
                .andExpect(jsonPath("$.data.users[0].id").value(firstId))
                .andExpect(jsonPath("$.data.users[0].role").value("TITIPER"))
                .andExpect(jsonPath("$.data.users[1].id").value(secondId))
                .andExpect(jsonPath("$.data.notFoundIds.length()").value(1))
                .andExpect(jsonPath("$.data.notFoundIds[0]").value(999999));
    }

    @Test
    void shouldRejectBulkLookupWhenUserIdsEmpty() throws Exception {
        String token = registerAndLogin("bulk_empty", "bulk_empty@example.com", "password123");

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
                String token = registerAndLogin("bulk_null", "bulk_null@example.com", "password123");

                mockMvc.perform(post("/api/profile/lookup/bulk")
                                                .header("Authorization", "Bearer " + token)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"userIds\":[1,null,2]}"))
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
        registerAndLogin("upgrade_target", "upgrade_target@example.com", "password123");
        Long targetUserId = userProfileRepository.findByEmail("upgrade_target@example.com")
                .orElseThrow(() -> new AssertionError("User target harus ada")).getId();

        String adminToken = jwtUtil.generateToken("admin_test@example.com", "ADMIN");

        Map<String, Object> request = Map.of("userId", targetUserId);

        mockMvc.perform(put("/api/profile/admin/role/upgrade")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role user berhasil di-upgrade ke JASTIPER!"))
                .andExpect(jsonPath("$.data.userId").value(targetUserId))
                .andExpect(jsonPath("$.data.oldRole").value("TITIPER"))
                .andExpect(jsonPath("$.data.newRole").value("JASTIPER"));

        UserProfile updated = userProfileRepository.findById(targetUserId)
                .orElseThrow(() -> new AssertionError("User target harus tetap ada"));

        Assertions.assertEquals(UserRole.JASTIPER.name(), updated.getRole());
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

        Map<String, Object> request = Map.of("userId", 999999L);

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
        registerAndLogin("upgrade_non_admin", "upgrade_non_admin@example.com", "password123");
        Long targetUserId = userProfileRepository.findByEmail("upgrade_non_admin@example.com")
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
        void shouldRejectGetJastiperProfilesWithoutToken() throws Exception {
                mockMvc.perform(get("/api/profile/jastiper"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
        }

    @Test
    void shouldGetOnlyJastiperProfiles() throws Exception {
        String token = registerAndLogin("viewer_user", "viewer_user@example.com", "password123");

        UserProfile jastiper = new UserProfile();
        jastiper.setUsername("jastiper_one");
        jastiper.setEmail("jastiper_one@example.com");
        jastiper.setPassword("dummy");
        jastiper.setRole(UserRole.JASTIPER.name());
        jastiper.setKycStatus("PENDING");

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
                .andExpect(jsonPath("$.data[0].role").value("JASTIPER"));
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
        String token = registerAndLogin("kyc_user", "kyc_user@example.com", "password123");

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
        String token = registerAndLogin("kyc_invalid_user", "kyc_invalid_user@example.com", "password123");

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
        String token = registerAndLogin("list_user", "list_user@example.com", "password123");

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

        mockMvc.perform(get("/api/profile/all")
                        .header("Authorization", "Bearer " + token))
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
                "password", "password123"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("TITIPER"))
                .andExpect(jsonPath("$.data.kycStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.username").isString())
                .andReturn();

        String generatedUsername = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .path("data").path("username").asText();

        Map<String, String> loginRequest = Map.of(
                "email", "milestone50@example.com",
                "password", "password123"
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
        String token = registerAndLogin("db_profile_user", "db_profile_user@example.com", "password123");

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
        String token = registerAndLogin("db_kyc_user", "db_kyc_user@example.com", "password123");

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
}




