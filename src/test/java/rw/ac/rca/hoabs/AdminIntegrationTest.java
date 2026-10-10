package rw.ac.rca.hoabs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import rw.ac.rca.hoabs.admin.CreateDoctorRequest;
import rw.ac.rca.hoabs.appointment.AppointmentRequest;
import rw.ac.rca.hoabs.department.Department;
import rw.ac.rca.hoabs.department.DepartmentDto;
import rw.ac.rca.hoabs.department.DepartmentRepository;
import rw.ac.rca.hoabs.doctor.Doctor;
import rw.ac.rca.hoabs.doctor.DoctorRepository;
import rw.ac.rca.hoabs.identity.RegisterRequest;
import rw.ac.rca.hoabs.identity.User;
import rw.ac.rca.hoabs.identity.UserRepository;
import rw.ac.rca.hoabs.security.JwtService;
import rw.ac.rca.hoabs.slot.Slot;
import rw.ac.rca.hoabs.slot.SlotRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Admin, Reception & Security Integration Tests (AD-001..AD-008, NF-001..NF-010)")
class AdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String patientToken;
    private User admin;
    private Department cardio;

    @BeforeEach
    void setUp() {
        admin = userRepository.findByPhone("0788000000").orElseThrow();
        User patientA = userRepository.findByPhone("0788000001").orElseThrow();
        cardio = departmentRepository.findByNameIgnoreCase("Cardiology").orElseThrow();

        adminToken = "Bearer " + jwtService.generateToken(admin);
        patientToken = "Bearer " + jwtService.generateToken(patientA);
    }

    @Test
    @DisplayName("AD-001: Admin creates new doctor profile successfully")
    void testAdminCreatesDoctor() throws Exception {
        CreateDoctorRequest req = new CreateDoctorRequest(
                "DOC-009", "Dr. Fabrice Habimana", "0788991122", "fabrice@amahoro.gov.rw",
                "Password1", cardio.getId(), "Cardiology", 20, 0L);

        mockMvc.perform(post("/api/v1/admin/doctors")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.staffId", is("DOC-009")))
                .andExpect(jsonPath("$.fullName", is("Dr. Fabrice Habimana")));
    }

    @Test
    @DisplayName("AD-002: Duplicate doctor staff ID is rejected (FR-ADM-04)")
    void testDuplicateStaffIdRejected() throws Exception {
        CreateDoctorRequest req = new CreateDoctorRequest(
                "DOC-001", "Duplicate Patrick", "0788991133", "dup@amahoro.gov.rw",
                "Password1", cardio.getId(), "Cardiology", 20, 0L);

        mockMvc.perform(post("/api/v1/admin/doctors")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("already exists")));
    }

    @Test
    @DisplayName("AD-003: Deactivating a doctor hides them without losing history")
    void testDeactivateDoctor() throws Exception {
        Doctor doc3 = doctorRepository.findByStaffId("DOC-003").orElseThrow();

        mockMvc.perform(post("/api/v1/admin/doctors/" + doc3.getId() + "/deactivate")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));

        // Doctor is hidden from patient search
        mockMvc.perform(get("/api/v1/doctors").param("query", "DOC-003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("AD-004: Admin edits department opening hours")
    void testUpdateDepartmentHours() throws Exception {
        DepartmentDto dto = new DepartmentDto(
                cardio.getId(), cardio.getName(), cardio.getDescription(),
                LocalTime.of(7, 30), LocalTime.of(16, 30), true);

        mockMvc.perform(put("/api/v1/admin/departments/" + cardio.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingTime", is("07:30:00")));
    }

    @Test
    @DisplayName("AD-005 / SEC-02: Non-admin cannot reach admin console (403 Forbidden)")
    void testPatientCannotAccessAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/doctors")
                        .header("Authorization", patientToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("access-denied")));
    }

    @Test
    @DisplayName("AD-006: Receptionist registers walk-in patient")
    void testReceptionistRegistersWalkIn() throws Exception {
        RegisterRequest req = new RegisterRequest(
                "Walkin Patient", "0788776655", "walkin@example.rw", "Password1", LocalDate.of(1985, 2, 2), null);

        mockMvc.perform(post("/api/v1/reception/patients")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.fullName", is("Walkin Patient")));
    }

    @Test
    @DisplayName("AD-007 / AD-008: Receptionist overrides 2-hour cut-off with logged reason")
    void testReceptionistOverridesTwoHourCutOff() throws Exception {
        Doctor doc1 = doctorRepository.findByStaffId("DOC-001").orElseThrow();
        User patientA = userRepository.findByPhone("0788000001").orElseThrow();

        // Create slot starting in 45 minutes
        Slot nearSlot = new Slot();
        nearSlot.setDoctor(doc1);
        nearSlot.setStartTime(Instant.now().plus(45, ChronoUnit.MINUTES));
        nearSlot.setEndTime(Instant.now().plus(65, ChronoUnit.MINUTES));
        nearSlot.setIsOpen(true);
        Slot savedNearSlot = slotRepository.save(nearSlot);

        AppointmentRequest req = new AppointmentRequest(
                savedNearSlot.getId(), "Severe acute chest pain", null, patientA.getId(), "Emergency triage doctor approved");

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.overrideReason", is("Emergency triage doctor approved")));

        // Verify audit log has the record
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].action", hasItem("OVERRIDE_2H_BOOKING")));
    }

    @Test
    @DisplayName("AD-008 / FR-RPT-01..03: Admin views reports and exports CSV")
    void testAdminReportsAndCsv() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAppointments", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.departmentCounts", notNullValue()));

        mockMvc.perform(get("/api/v1/admin/reports/csv")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"))
                .andExpect(content().string(containsString("Department,Status,Count")));
    }

    @Test
    @DisplayName("NF-008 / SEC-05: SQL injection attempt on search is safely handled as literal string")
    void testSqlInjectionSafeHandling() throws Exception {
        mockMvc.perform(get("/api/v1/doctors").param("query", "' OR '1'='1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
