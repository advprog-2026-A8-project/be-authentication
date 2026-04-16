package id.ac.ui.cs.advprog.beauthentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.beauthentication.repository.UserProfileRepository;
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
}
