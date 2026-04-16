package id.ac.ui.cs.advprog.beauthentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.beauthentication.model.UserProfile;
import id.ac.ui.cs.advprog.beauthentication.repository.UserProfileRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
    void shouldRejectProfileUpdateWhenUsernameAlreadyUsed() throws Exception {
        registerAndLogin("first_user", "first_user@example.com", "password123");
        String secondToken = registerAndLogin("second_user", "second_user@example.com", "password123");

        Map<String, String> updateRequest = Map.of(
                "username", "first_user"
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
                "username", "token_stable_user_updated"
        );

        mockMvc.perform(put("/api/profile/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("token_stable_user_updated"));

        mockMvc.perform(get("/api/profile/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("token_stable_user_updated"));
    }

    @Test
    void shouldRejectGetMyProfileWithoutToken() throws Exception {
        mockMvc.perform(get("/api/profile/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUpdateMyProfileWithoutToken() throws Exception {
        Map<String, String> updateRequest = Map.of(
                "fullName", "No Token User"
        );

        mockMvc.perform(put("/api/profile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
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
                .andExpect(status().isForbidden());
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
