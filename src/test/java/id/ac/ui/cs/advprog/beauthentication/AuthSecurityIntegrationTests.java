package id.ac.ui.cs.advprog.beauthentication;

import id.ac.ui.cs.advprog.beauthentication.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void shouldRejectProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/profile/all"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowProtectedEndpointWithValidJwt() throws Exception {
        String token = jwtUtil.generateToken("asdos_reviewer");

        mockMvc.perform(get("/api/profile/all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectProtectedEndpointWithInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/profile/all")
                        .header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isForbidden());
    }
}
