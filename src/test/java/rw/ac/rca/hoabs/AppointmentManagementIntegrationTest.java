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
import rw.ac.rca.hoabs.appointment.*;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Appointment Management Integration Tests (AM-001..AM-012, FR-APPT-*)")
class AppointmentManagementIntegrationTest {

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
    private AppointmentRepository appointmentRepository;

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

    private Appointment createTestAppointment(User patient, Instant slotTime) {
        Slot s = new Slot();
        s.setDoctor(doc1);
        s.setStartTime(slotTime);
        s.setEndTime(slotTime.plus(20, ChronoUnit.MINUTES));
        s.setIsOpen(false);
        Slot savedSlot = slotRepository.save(s);

        Appointment a = new Appointment();
        a.setReferenceNo("TEST-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        a.setPatient(patient);
        a.setDoctor(doc1);
        a.setSlot(savedSlot);
        a.setReason("General Consultation");
        a.setStatus(AppointmentStatus.CONFIRMED);
        return appointmentRepository.save(a);
    }

    @Test
    @DisplayName("AM-001: Patient cancels upcoming appointment, frees slot, dispatches notice")
    void testCancelUpcomingAppointment() throws Exception {
        Appointment appt = createTestAppointment(patientA, Instant.now().plus(48, ChronoUnit.HOURS));

        CancelRequest req = new CancelRequest("Conflict with personal schedule", null);

        mockMvc.perform(post("/api/v1/appointments/" + appt.getId() + "/cancel")
                        .header("Authorization", patientAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        // Slot is freed immediately
        Slot freedSlot = slotRepository.findById(appt.getSlot().getId()).orElseThrow();
        assertTrue(freedSlot.isOpen(), "Cancelled appointment slot must be freed immediately");

        // AM-008: Cancellation notification recorded
        List<Notification> notifs = notificationRepository.findByRecipientOrderBySentAtDesc(patientA.getPhone());
        assertTrue(notifs.stream().anyMatch(n -> n.getMessage().contains("cancelled")), "Cancellation notice must be recorded");
    }

    @Test
    @DisplayName("AM-002 / AM-010: Patient reschedules to new open slot atomically (Single Confirmed record)")
    void testRescheduleAppointment() throws Exception {
        Appointment appt = createTestAppointment(patientA, Instant.now().plus(24, ChronoUnit.HOURS));

        // Create new open slot
        Slot newSlot = new Slot();
        newSlot.setDoctor(doc1);
        newSlot.setStartTime(Instant.now().plus(72, ChronoUnit.HOURS));
        newSlot.setEndTime(Instant.now().plus(72, ChronoUnit.HOURS).plus(20, ChronoUnit.MINUTES));
        newSlot.setIsOpen(true);
        Slot savedNewSlot = slotRepository.save(newSlot);

        RescheduleRequest req = new RescheduleRequest(savedNewSlot.getId(), null);

        mockMvc.perform(post("/api/v1/appointments/" + appt.getId() + "/reschedule")
                        .header("Authorization", patientAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.slotId", is(savedNewSlot.getId().toString())));

        // Old slot is freed
        Slot oldSlot = slotRepository.findById(appt.getSlot().getId()).orElseThrow();
        assertTrue(oldSlot.isOpen(), "Old slot must be freed after rescheduling");
    }

    @Test
    @DisplayName("AM-003: Cancellation within 2 hours is blocked online")
    void testCancelWithinTwoHoursBlocked() throws Exception {
        Appointment urgent = createTestAppointment(patientA, Instant.now().plus(1, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/v1/appointments/" + urgent.getId() + "/cancel")
                        .header("Authorization", patientAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("blocked within 2 hours")));
    }

    @Test
    @DisplayName("AM-006 / SEC-03: Patient A cannot access Patient B's appointment (IDOR Protection: 404 Not Found)")
    void testIdorProtectionReturns404() throws Exception {
        Appointment patientBAppt = createTestAppointment(patientB, Instant.now().plus(24, ChronoUnit.HOURS));

        // Patient A attempts to view Patient B's appointment directly by ID
        mockMvc.perform(get("/api/v1/appointments/" + patientBAppt.getId())
                        .header("Authorization", patientAToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("not-found")));

        // Patient A attempts to cancel Patient B's appointment
        mockMvc.perform(post("/api/v1/appointments/" + patientBAppt.getId() + "/cancel")
                        .header("Authorization", patientAToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("AM-007: Patient views full appointment history")
    void testViewAppointmentHistory() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/my")
                        .header("Authorization", patientBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[0].patientName", is("Patient Beta")));
    }
}
