package id.ac.ui.cs.advprog.beauthentication;

import id.ac.ui.cs.advprog.beauthentication.utils.JwtUtil;
import org.junit.jupiter.api.Assertions;
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
                .andExpect(status().isForbidden());
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
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowJastiperEndpointWithValidJwt() throws Exception {
        String token = jwtUtil.generateToken("asdos_reviewer", "ADMIN");

        mockMvc.perform(get("/api/profile/jastiper")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectJastiperEndpointWithInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/profile/jastiper")
                        .header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectVerifyEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/verify"))
                .andExpect(status().isForbidden());
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
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectAdminRoleUpgradeWhenTokenHasNoRoleClaim() throws Exception {
        String legacyTokenWithoutRole = jwtUtil.generateToken("legacy_user@example.com");

        mockMvc.perform(put("/api/profile/admin/role/upgrade")
                        .header("Authorization", "Bearer " + legacyTokenWithoutRole)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1}"))
                .andExpect(status().isForbidden());
    }
}
