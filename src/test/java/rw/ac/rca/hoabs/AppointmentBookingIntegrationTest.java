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
import rw.ac.rca.hoabs.appointment.AppointmentRequest;
import rw.ac.rca.hoabs.doctor.Doctor;
import rw.ac.rca.hoabs.doctor.DoctorRepository;
import rw.ac.rca.hoabs.identity.User;
import rw.ac.rca.hoabs.identity.UserRepository;
import rw.ac.rca.hoabs.notification.Notification;
import rw.ac.rca.hoabs.notification.NotificationRepository;
import rw.ac.rca.hoabs.security.JwtService;
import rw.ac.rca.hoabs.slot.Slot;
import rw.ac.rca.hoabs.slot.SlotRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Appointment Booking Integration Tests (AB-001..AB-012, FR-BOOK-*)")
class AppointmentBookingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtService jwtService;

    private String patientAToken;
    private String patientBToken;
    private User patientA;
    private User patientB;
    private Doctor doc1;

    @BeforeEach
    void setUp() {
        patientA = userRepository.findByPhone("0788000001").orElseThrow();
        patientB = userRepository.findByPhone("0788000002").orElseThrow();
        doc1 = doctorRepository.findByStaffId("DOC-001").orElseThrow();

        patientAToken = "Bearer " + jwtService.generateToken(patientA);
        patientBToken = "Bearer " + jwtService.generateToken(patientB);
    }

    @Test
    @DisplayName("AB-001: Patient A books open slot successfully (Happy Path)")
    void testPatientABooksOpenSlot() throws Exception {
        // Find an open slot for DOC-001 at least 3 hours in the future
        Slot openSlot = slotRepository.findFutureSlotsForDoctor(doc1.getId(), Instant.now().plus(3, ChronoUnit.HOURS))
                .stream().filter(Slot::isOpen).findFirst().orElseThrow();

        AppointmentRequest req = new AppointmentRequest(openSlot.getId(), "Chest tightness and palpitations", "MTN_MOMO", null, null);

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", patientAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.referenceNo", startsWith("APT-")))
                .andExpect(jsonPath("$.patientName", is("Patient Alpha")));

        // Verify slot is no longer open
        Slot updatedSlot = slotRepository.findById(openSlot.getId()).orElseThrow();
        assertFalse(updatedSlot.isOpen(), "Booked slot must be marked closed");

        // AB-009: Verify confirmation notification was recorded
        List<Notification> notifs = notificationRepository.findByRecipientOrderBySentAtDesc("0788000001");
        assertTrue(notifs.stream().anyMatch(n -> n.getMessage().contains("confirmed")), "Confirmation notification must be dispatched");
    }

    @Test
    @DisplayName("AB-002 / NFR-REL-01: Two patients cannot book the same slot (Concurrency Guard)")
    void testTwoPatientsCannotBookSameSlot() throws Exception {
        Slot openSlot = slotRepository.findFutureSlotsForDoctor(doc1.getId(), Instant.now().plus(5, ChronoUnit.HOURS))
                .stream().filter(Slot::isOpen).findFirst().orElseThrow();

        AppointmentRequest req1 = new AppointmentRequest(openSlot.getId(), "Patient 1 appointment", null, null, null);
        AppointmentRequest req2 = new AppointmentRequest(openSlot.getId(), "Patient 2 attempt on same slot", null, null, null);

        // Create a distinct second patient (Patient C)
        User patientC = new User();
        patientC.setFullName("Patient Charlie");
        patientC.setPhone("0788334455");
        patientC.setEmail("patient.c@example.com");
        patientC.setPasswordHash("hash");
        patientC.setRole(rw.ac.rca.hoabs.identity.Role.PATIENT);
        patientC.setStatus(rw.ac.rca.hoabs.identity.UserStatus.ACTIVE);
        User savedPatientC = userRepository.save(patientC);
        String patientCToken = "Bearer " + jwtService.generateToken(savedPatientC);

        // First patient succeeds
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", patientAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Second patient is rejected because slot is already taken
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", patientCToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("This slot was just booked — pick another")));
    }

    @Test
    @DisplayName("AB-003: Patient B 4th upcoming booking is rejected (Booking Limit Rule)")
    void testPatientBBookingLimitRule() throws Exception {
        Slot openSlot = slotRepository.findFutureSlotsForDoctor(doc1.getId(), Instant.now().plus(6, ChronoUnit.HOURS))
                .stream().filter(Slot::isOpen).findFirst().orElseThrow();

        AppointmentRequest req = new AppointmentRequest(openSlot.getId(), "4th consultation attempt", null, null, null);

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", patientBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("Booking limit reached")));
    }

    @Test
    @DisplayName("AB-004: Booking within 2 hours of slot start is blocked online")
    void testLeadTimeUnderTwoHoursBlocked() throws Exception {
        // Create a slot starting in 30 minutes
        Slot urgentSlot = new Slot();
        urgentSlot.setDoctor(doc1);
        urgentSlot.setStartTime(Instant.now().plus(30, ChronoUnit.MINUTES));
        urgentSlot.setEndTime(Instant.now().plus(50, ChronoUnit.MINUTES));
        urgentSlot.setIsOpen(true);
        Slot savedUrgent = slotRepository.save(urgentSlot);

        AppointmentRequest req = new AppointmentRequest(savedUrgent.getId(), "Urgent check", null, null, null);

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", patientAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("at least 2 hours in advance")));
    }

    @Test
    @DisplayName("AB-005: Reason-for-visit is required (Validation)")
    void testReasonRequiredValidation() throws Exception {
        Slot openSlot = slotRepository.findFutureSlotsForDoctor(doc1.getId(), Instant.now().plus(7, ChronoUnit.HOURS))
                .stream().filter(Slot::isOpen).findFirst().orElseThrow();

        AppointmentRequest req = new AppointmentRequest(openSlot.getId(), "   ", null, null, null);

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", patientAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason", notNullValue()));
    }

    @Test
    @DisplayName("AB-010: Guest prompted to log in before booking (401 Unauthorized)")
    void testUnauthenticatedGuestCannotBook() throws Exception {
        AppointmentRequest req = new AppointmentRequest(java.util.UUID.randomUUID(), "Guest booking", null, null, null);

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
