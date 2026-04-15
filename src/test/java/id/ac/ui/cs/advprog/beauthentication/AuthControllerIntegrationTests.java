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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTests {

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

    @Test
    void shouldRegisterSuccessfullyWithoutReturningPassword() throws Exception {
        Map<String, String> registerRequest = Map.of(
                "username", "new_user",
                "email", "new_user@example.com",
                "password", "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registrasi berhasil!"))
                .andExpect(jsonPath("$.data.username").value("new_user"))
                .andExpect(jsonPath("$.data.email").value("new_user@example.com"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void shouldRejectDuplicateRegisterByUsername() throws Exception {
        Map<String, String> firstRegister = Map.of(
                "username", "duplicate_user",
                "email", "dup1@example.com",
                "password", "password123"
        );

        Map<String, String> secondRegister = Map.of(
                "username", "duplicate_user",
                "email", "dup2@example.com",
                "password", "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRegister)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRegister)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username atau Email sudah terdaftar!"));
    }

    @Test
    void shouldLoginSuccessfullyWithEmailAndPassword() throws Exception {
        Map<String, String> registerRequest = Map.of(
                "username", "login_user",
                "email", "login_user@example.com",
                "password", "password123"
        );

        Map<String, String> loginRequest = Map.of(
                "email", "LOGIN_USER@EXAMPLE.COM",
                "password", "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login berhasil!"))
                .andExpect(jsonPath("$.data.token").isString());
    }

    @Test
    void shouldRejectLoginWithWrongPasswordAsUnauthorized() throws Exception {
        Map<String, String> registerRequest = Map.of(
                "username", "wrong_pass_user",
                "email", "wrong_pass@example.com",
                "password", "password123"
        );

        Map<String, String> loginRequest = Map.of(
                "email", "wrong_pass@example.com",
                "password", "not_the_right_password"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Password salah!"));
    }

    @Test
    void shouldRejectLoginWithInvalidEmailFormatAsBadRequest() throws Exception {
        Map<String, String> loginRequest = Map.of(
                "email", "invalid-email-format",
                "password", "password123"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Format email tidak valid!"));
    }
}
