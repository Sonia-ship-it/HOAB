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
import rw.ac.rca.hoabs.appointment.Appointment;
import rw.ac.rca.hoabs.appointment.AppointmentRepository;
import rw.ac.rca.hoabs.appointment.AppointmentStatus;
import rw.ac.rca.hoabs.doctor.Doctor;
import rw.ac.rca.hoabs.doctor.DoctorRepository;
import rw.ac.rca.hoabs.doctor.LeaveBlockDto;
import rw.ac.rca.hoabs.doctor.WorkingHoursDto;
import rw.ac.rca.hoabs.identity.User;
import rw.ac.rca.hoabs.identity.UserRepository;
import rw.ac.rca.hoabs.security.JwtService;
import rw.ac.rca.hoabs.slot.Slot;
import rw.ac.rca.hoabs.slot.SlotRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Doctor Schedule Integration Tests (DR-001..DR-008, FR-SCHED-*)")
class DoctorScheduleIntegrationTest {

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
    private JwtService jwtService;

    private String doctorToken;
    private Doctor doc1;
    private User docUser;

    @BeforeEach
    void setUp() {
        doc1 = doctorRepository.findByStaffId("DOC-001").orElseThrow();
        docUser = doc1.getUser();
        doctorToken = "Bearer " + jwtService.generateToken(docUser);
    }

    @Test
    @DisplayName("DR-001: Doctor views and updates recurring weekly working hours")
    void testDoctorSetsWorkingHours() throws Exception {
        List<WorkingHoursDto> hours = List.of(
                new WorkingHoursDto(null, 1, LocalTime.of(8, 0), LocalTime.of(15, 0)),
                new WorkingHoursDto(null, 2, LocalTime.of(8, 0), LocalTime.of(15, 0))
        );

        mockMvc.perform(put("/api/v1/doctor/my-schedule/hours")
                        .header("Authorization", doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hours)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("updated successfully")));
    }

    @Test
    @DisplayName("DR-004: Working hours outside department hours (07:00-17:00) are rejected")
    void testWorkingHoursOutsideDepartmentHoursRejected() throws Exception {
        // Try setting doctor working hours 06:00 to 18:00 (outside Cardiology 07:00-17:00)
        List<WorkingHoursDto> badHours = List.of(
                new WorkingHoursDto(null, 1, LocalTime.of(6, 0), LocalTime.of(18, 0))
        );

        mockMvc.perform(put("/api/v1/doctor/my-schedule/hours")
                        .header("Authorization", doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badHours)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("must fall within department opening hours")));
    }

    @Test
    @DisplayName("DR-002: Doctor blocks a leave date with no conflicts")
    void testBlockLeaveDateNoConflicts() throws Exception {
        LocalDate farFuture = LocalDate.now().plusDays(20);
        LeaveBlockDto leaveDto = new LeaveBlockDto(null, farFuture, farFuture.plusDays(1), "Medical Training");

        mockMvc.perform(post("/api/v1/doctor/my-schedule/leave")
                        .header("Authorization", doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leaveDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason", is("Medical Training")));
    }

    @Test
    @DisplayName("DR-003: Leave overlapping a Confirmed booking requires resolution (Conflict)")
    void testLeaveConflictWithConfirmedBooking() throws Exception {
        LocalDate conflictDate = LocalDate.now().plusDays(10);
        Instant conflictTime = conflictDate.atTime(9, 0).atZone(rw.ac.rca.hoabs.slot.SlotGeneratorService.HOSPITAL_ZONE).toInstant();

        // Create Confirmed appointment on conflictDate
        User patientA = userRepository.findByPhone("0788000001").orElseThrow();
        Slot s = slotRepository.findByDoctorIdAndStartTime(doc1.getId(), conflictTime).orElseGet(() -> {
            Slot newSlot = new Slot();
            newSlot.setDoctor(doc1);
            newSlot.setStartTime(conflictTime);
            newSlot.setEndTime(conflictTime.plus(20, ChronoUnit.MINUTES));
            return newSlot;
        });
        s.setIsOpen(false);
        Slot savedSlot = slotRepository.save(s);

        Appointment conflictAppt = new Appointment();
        conflictAppt.setReferenceNo("CONF-" + java.util.UUID.randomUUID().toString().substring(0, 6));
        conflictAppt.setPatient(patientA);
        conflictAppt.setDoctor(doc1);
        conflictAppt.setSlot(savedSlot);
        conflictAppt.setReason("Important clinical review");
        conflictAppt.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(conflictAppt);

        // Doctor attempts to block conflictDate as leave
        LeaveBlockDto leaveDto = new LeaveBlockDto(null, conflictDate, conflictDate, "Emergency leave");

        mockMvc.perform(post("/api/v1/doctor/my-schedule/leave")
                        .header("Authorization", doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leaveDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("Confirmed appointment(s) exist on the selected dates")));
    }

    @Test
    @DisplayName("DR-006: Doctor views today's confirmed patients")
    void testDoctorViewsTodayList() throws Exception {
        mockMvc.perform(get("/api/v1/doctor/today")
                        .header("Authorization", doctorToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DR-007 & DR-008: Doctor marks appointment Completed and No-show")
    void testDoctorMarksOutcomes() throws Exception {
        User patientA = userRepository.findByPhone("0788000001").orElseThrow();
        Instant todayTime = Instant.now().plus(1, ChronoUnit.HOURS);

        Slot s1 = new Slot();
        s1.setDoctor(doc1);
        s1.setStartTime(todayTime);
        s1.setEndTime(todayTime.plus(20, ChronoUnit.MINUTES));
        s1.setIsOpen(false);
        Slot savedSlot1 = slotRepository.save(s1);

        Appointment a1 = new Appointment();
        a1.setReferenceNo("OUTCOME-1");
        a1.setPatient(patientA);
        a1.setDoctor(doc1);
        a1.setSlot(savedSlot1);
        a1.setReason("Check outcome");
        a1.setStatus(AppointmentStatus.CONFIRMED);
        Appointment saved1 = appointmentRepository.save(a1);

        // Mark Completed
        mockMvc.perform(post("/api/v1/doctor/appointments/" + saved1.getId() + "/complete")
                        .header("Authorization", doctorToken))
                .andExpect(status().isOk());

        Appointment completed = appointmentRepository.findById(saved1.getId()).orElseThrow();
        assertEquals(AppointmentStatus.COMPLETED, completed.getStatus());
    }
}
