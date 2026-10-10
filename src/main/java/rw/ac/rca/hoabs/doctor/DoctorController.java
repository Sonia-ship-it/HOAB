package rw.ac.rca.hoabs.doctor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rw.ac.rca.hoabs.appointment.AppointmentResponse;
import rw.ac.rca.hoabs.appointment.AppointmentStatus;
import rw.ac.rca.hoabs.common.exception.ApiException;
import rw.ac.rca.hoabs.security.UserPrincipal;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Doctors & Schedule", description = "Endpoints for doctor discovery and doctor console scheduling")
public class DoctorController {

    private final DoctorService doctorService;
    private final DoctorRepository doctorRepository;

    public DoctorController(DoctorService doctorService, DoctorRepository doctorRepository) {
        this.doctorService = doctorService;
        this.doctorRepository = doctorRepository;
    }

    private Doctor getDoctorForPrincipal(UserPrincipal principal) {
        return doctorRepository.findByUserId(principal.getId())
                .orElseThrow(() -> ApiException.forbidden("Current user is not registered as a doctor."));
    }

    // Public doctor browsing
    @GetMapping("/doctors")
    @Operation(summary = "Search active doctors", description = "Browse by department or query (specialty/name). Only active doctors returned.")
    public ResponseEntity<List<DoctorResponse>> searchDoctors(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(doctorService.searchActiveDoctors(departmentId, query));
    }

    @GetMapping("/doctors/{id}")
    @Operation(summary = "Get doctor details", description = "Public profile with next available appointment slot")
    public ResponseEntity<DoctorResponse> getDoctor(@PathVariable UUID id) {
        return ResponseEntity.ok(doctorService.getDoctorProfile(id));
    }

    // Doctor Console endpoints
    @GetMapping("/doctor/my-schedule")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Get doctor schedule", description = "View working hours, slot duration, and leave blocks")
    public ResponseEntity<DoctorScheduleDto> getMySchedule(@AuthenticationPrincipal UserPrincipal principal) {
        Doctor doc = getDoctorForPrincipal(principal);
        return ResponseEntity.ok(doctorService.getDoctorSchedule(doc.getId()));
    }

    @PutMapping("/doctor/my-schedule/hours")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Update working hours", description = "Set recurring weekly hours; validates against department hours")
    public ResponseEntity<Map<String, String>> updateWorkingHours(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody List<WorkingHoursDto> hours,
            @RequestParam(required = false) Integer slotDuration) {
        Doctor doc = getDoctorForPrincipal(principal);
        doctorService.setWorkingHours(doc.getId(), hours, slotDuration);
        return ResponseEntity.ok(Map.of("message", "Working hours and slot duration updated successfully."));
    }

    @PostMapping("/doctor/my-schedule/leave")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Block leave dates", description = "Blocks leave range; validates no Confirmed appointments overlap")
    public ResponseEntity<LeaveBlockDto> addLeave(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody LeaveBlockDto leaveDto) {
        Doctor doc = getDoctorForPrincipal(principal);
        LeaveBlockDto created = doctorService.addLeaveBlock(doc.getId(), leaveDto);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/doctor/today")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Today's patient list", description = "Lists today's Confirmed appointments in chronological order")
    public ResponseEntity<List<AppointmentResponse>> getTodayList(@AuthenticationPrincipal UserPrincipal principal) {
        Doctor doc = getDoctorForPrincipal(principal);
        List<AppointmentResponse> list = doctorService.getTodayAppointments(doc.getId())
                .stream()
                .map(AppointmentResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/doctor/appointments/{id}/complete")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Mark appointment Completed", description = "Doctor marks visit done (FR-SCHED-07)")
    public ResponseEntity<Map<String, String>> markCompleted(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        Doctor doc = getDoctorForPrincipal(principal);
        doctorService.markAppointmentOutcome(doc.getId(), id, AppointmentStatus.COMPLETED);
        return ResponseEntity.ok(Map.of("message", "Appointment marked as COMPLETED."));
    }

    @PostMapping("/doctor/appointments/{id}/no-show")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Mark appointment No-show", description = "Doctor marks patient as no-show (FR-SCHED-07)")
    public ResponseEntity<Map<String, String>> markNoShow(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        Doctor doc = getDoctorForPrincipal(principal);
        doctorService.markAppointmentOutcome(doc.getId(), id, AppointmentStatus.NO_SHOW);
        return ResponseEntity.ok(Map.of("message", "Appointment marked as NO_SHOW."));
    }
}
