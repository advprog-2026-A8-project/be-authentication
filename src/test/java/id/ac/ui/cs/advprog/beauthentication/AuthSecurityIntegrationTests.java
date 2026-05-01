package id.ac.ui.cs.advprog.beauthentication;

import id.ac.ui.cs.advprog.beauthentication.model.AccountStatus;
import id.ac.ui.cs.advprog.beauthentication.model.KycStatus;
import id.ac.ui.cs.advprog.beauthentication.model.UserProfile;
import id.ac.ui.cs.advprog.beauthentication.model.UserRole;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @BeforeEach
    void setUp() {
        userProfileRepository.deleteAll();
    }

    @Test
    void shouldAllowRegisterEndpointWithoutToken() throws Exception {
        String email = "security_register_" + System.nanoTime() + "@example.com";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowLoginEndpointWithoutToken() throws Exception {
        String email = "security_login_" + System.nanoTime() + "@example.com";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString());
    }

    @Test
    void shouldRejectProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/profile/all"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void shouldAllowPublicProfileLookupWithoutToken() throws Exception {
        UserProfile user = new UserProfile();
        user.setUsername("public_lookup");
        user.setEmail("public_lookup@example.com");
        user.setPassword("dummy");
        user.setRole(UserRole.TITIPER.name());
        user.setKycStatus(KycStatus.PENDING.name());
        userProfileRepository.save(user);

        mockMvc.perform(get("/api/profile/lookup")
                        .param("email", "public_lookup@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profil berhasil ditemukan!"))
                .andExpect(jsonPath("$.data.username").value("public_lookup"));
    }

    @Test
    void shouldAllowPublicJastiperListWithoutToken() throws Exception {
        UserProfile jastiper = new UserProfile();
        jastiper.setUsername("public_jastiper");
        jastiper.setEmail("public_jastiper@example.com");
        jastiper.setPassword("dummy");
        jastiper.setRole(UserRole.JASTIPER.name());
        jastiper.setKycStatus(KycStatus.APPROVED.name());
        userProfileRepository.save(jastiper);

        mockMvc.perform(get("/api/profile/jastiper"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Daftar jastiper berhasil diambil!"))
                .andExpect(jsonPath("$.data[0].username").value("public_jastiper"))
                .andExpect(jsonPath("$.data[0].role").value("JASTIPER"));
    }

    @Test
    void shouldRejectKycSubmissionWithoutToken() throws Exception {
        mockMvc.perform(post("/api/profile/kyc/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Budi\",\"identityDocumentUrl\":\"doc\",\"socialMediaUrl\":\"ig\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void shouldAllowProtectedEndpointWithValidJwt() throws Exception {
        String token = jwtUtil.generateToken("asdos_reviewer", "ADMIN");

        mockMvc.perform(get("/api/profile/all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        Assertions.assertEquals("ADMIN", jwtUtil.extractRole(token));
    }

    @Test
    void shouldRejectProtectedEndpointWithInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/profile/all")
                        .header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void shouldAllowJastiperEndpointWithValidJwt() throws Exception {
        String token = jwtUtil.generateToken("asdos_reviewer", "ADMIN");

        mockMvc.perform(get("/api/profile/jastiper")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowJastiperEndpointWithInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/profile/jastiper")
                        .header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectVerifyEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/verify"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void shouldAllowVerifyEndpointWithValidJwt() throws Exception {
        String token = jwtUtil.generateToken("verify_user@example.com", "JASTIPER");

        mockMvc.perform(get("/api/auth/verify")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token valid!"))
                .andExpect(jsonPath("$.data.subject").value("verify_user@example.com"))
                .andExpect(jsonPath("$.data.role").value("JASTIPER"))
                .andExpect(jsonPath("$.data.expiresAt").isNumber());
    }

    @Test
    void shouldRejectVerifyEndpointWithInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/auth/verify")
                        .header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void shouldRejectAdminRoleUpgradeWhenTokenHasNoRoleClaim() throws Exception {
        String legacyTokenWithoutRole = jwtUtil.generateToken("legacy_user@example.com");

        mockMvc.perform(put("/api/profile/admin/role/upgrade")
                        .header("Authorization", "Bearer " + legacyTokenWithoutRole)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

            @Test
            void shouldBlockBannedUserFromProtectedEndpointsButAllowVerify() throws Exception {
            UserProfile bannedUser = new UserProfile();
            bannedUser.setUsername("banned_user");
            bannedUser.setEmail("banned_user@example.com");
            bannedUser.setPassword("dummy");
            bannedUser.setRole(UserRole.TITIPER.name());
            bannedUser.setAccountStatus(AccountStatus.BANNED.name());
            bannedUser.setKycStatus(KycStatus.PENDING.name());
            userProfileRepository.save(bannedUser);

            String token = jwtUtil.generateToken("banned_user@example.com", "TITIPER");

            mockMvc.perform(get("/api/profile/all")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));

            mockMvc.perform(get("/api/auth/verify")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token valid!"))
                .andExpect(jsonPath("$.data.subject").value("banned_user@example.com"));
            }
}
