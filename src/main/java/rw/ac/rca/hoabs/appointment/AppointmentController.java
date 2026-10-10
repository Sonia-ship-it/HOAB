package rw.ac.rca.hoabs.appointment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rw.ac.rca.hoabs.common.exception.ApiException;
import rw.ac.rca.hoabs.identity.User;
import rw.ac.rca.hoabs.identity.UserRepository;
import rw.ac.rca.hoabs.security.UserPrincipal;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments", description = "Endpoints for booking, rescheduling, cancelling, and viewing patient appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;

    public AppointmentController(AppointmentService appointmentService, UserRepository userRepository) {
        this.appointmentService = appointmentService;
        this.userRepository = userRepository;
    }

    private User getUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> ApiException.unauthorized("User session expired"));
    }

    @PostMapping
    @Operation(summary = "Book an appointment", description = "Reserves an open slot for a patient. Concurrency guard prevents double-booking.")
    public ResponseEntity<AppointmentResponse> bookAppointment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AppointmentRequest request,
            @RequestHeader(name = "X-Payment-Stub-Mode", required = false) String stubMode) {
        User caller = getUser(principal);
        AppointmentResponse response = appointmentService.bookAppointment(caller, request, stubMode);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    @Operation(summary = "View my appointment history", description = "Patient views full history of their upcoming and past appointments (FR-APPT-09)")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(@AuthenticationPrincipal UserPrincipal principal) {
        User caller = getUser(principal);
        List<AppointmentResponse> list = appointmentService.getPatientAppointments(caller.getId());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment details", description = "Returns appointment details. Returns 404 if caller is not the owner (IDOR protection).")
    public ResponseEntity<AppointmentResponse> getAppointment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        User caller = getUser(principal);
        AppointmentResponse response = appointmentService.getAppointmentById(caller, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel appointment", description = "Patient or staff cancels Confirmed appointment and frees slot immediately (FR-APPT-01)")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody(required = false) CancelRequest request) {
        User caller = getUser(principal);
        String reason = request != null ? request.reason() : null;
        String override = request != null ? request.overrideReason() : null;
        AppointmentResponse response = appointmentService.cancelAppointment(caller, id, reason, override);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reschedule")
    @Operation(summary = "Reschedule appointment", description = "Atomically frees old slot and reserves new slot in one operation (FR-APPT-03)")
    public ResponseEntity<AppointmentResponse> rescheduleAppointment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody RescheduleRequest request) {
        User caller = getUser(principal);
        AppointmentResponse response = appointmentService.rescheduleAppointment(caller, id, request);
        return ResponseEntity.ok(response);
    }
}
