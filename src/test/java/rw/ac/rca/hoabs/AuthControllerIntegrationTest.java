package rw.ac.rca.hoabs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import rw.ac.rca.hoabs.identity.LoginRequest;
import rw.ac.rca.hoabs.identity.RegisterRequest;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Authentication Integration Tests (AU-001..AU-012, FR-AUTH-*)")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("AU-001 (Smoke): Health and API status endpoint loads")
    void testSmokeHealth() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    @DisplayName("AU-002: Registered Patient A logs in successfully")
    void testPatientALoginHappyPath() throws Exception {
        LoginRequest req = new LoginRequest("0788000001", "Password1");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.user.role", is("PATIENT")))
                .andExpect(jsonPath("$.user.phone", is("0788000001")));
    }

    @Test
    @DisplayName("AU-003: Wrong password returns generic error without disclosing phone")
    void testWrongPasswordGenericError() throws Exception {
        LoginRequest req = new LoginRequest("0788000001", "WrongPassword99");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("invalid-credentials")))
                .andExpect(jsonPath("$.detail", containsString("Phone/email or password is incorrect")));
    }

    @Test
    @DisplayName("AU-004: Account locks after 3 failed attempts for 30 minutes")
    void testAccountLockoutAfterThreeFailures() throws Exception {
        String testPhone = "0788" + (int)(System.currentTimeMillis() % 900000 + 100000);
        RegisterRequest reg = new RegisterRequest(
                "Lockout Test User", testPhone, "lockout" + System.currentTimeMillis() + "@example.com", "Password1", LocalDate.of(1990, 1, 1), null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest bad = new LoginRequest(testPhone, "Wrong123");
        // 3 consecutive failed attempts
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isUnauthorized());

        // 4th attempt with CORRECT password is also rejected because account is locked
        LoginRequest correct = new LoginRequest(testPhone, "Password1");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correct)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail", containsString("Phone/email or password is incorrect")));
    }

    @Test
    @DisplayName("AU-005: Already-locked account rejected with generic error")
    void testLockedPatientLoginRejected() throws Exception {
        LoginRequest req = new LoginRequest("0788000003", "Password1");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail", containsString("Phone/email or password is incorrect")));
    }

    @Test
    @DisplayName("AU-006: New patient registers successfully")
    void testNewPatientRegistration() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "Jean Paul", "0788112233", "jp@example.rw", "SecretPass1", LocalDate.of(1992, 3, 15), "RAMA-001");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.user.fullName", is("Jean Paul")))
                .andExpect(jsonPath("$.user.role", is("PATIENT")));
    }

    @Test
    @DisplayName("AU-007: Duplicate phone registration rejected")
    void testDuplicatePhoneRegistrationRejected() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "Duplicate User", "0788000001", "other@example.com", "Password1", LocalDate.of(1990, 1, 1), null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("Phone number already registered")));
    }

    @Test
    @DisplayName("AU-008: Weak password rejected with validation error")
    void testWeakPasswordRejected() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "Weak Pass User", "0788554433", "weak@example.com", "pass", LocalDate.of(1990, 1, 1), null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    @Test
    @DisplayName("AU-011: Doctor login returns role DOCTOR")
    void testDoctorRoleLogin() throws Exception {
        LoginRequest req = new LoginRequest("0788000101", "Password1");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role", is("DOCTOR")));
    }

    @Test
    @DisplayName("AU-012: Admin login returns role ADMIN")
    void testAdminRoleLogin() throws Exception {
        LoginRequest req = new LoginRequest("0788000000", "Password1");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role", is("ADMIN")));
    }
}
