package rw.ac.rca.hoabs.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.ac.rca.hoabs.department.DepartmentDto;
import rw.ac.rca.hoabs.department.DepartmentService;
import rw.ac.rca.hoabs.doctor.DoctorResponse;
import rw.ac.rca.hoabs.identity.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Admin & Reception", description = "Endpoints for hospital administration, doctor/dept master data, walk-in registration and reports")
public class AdminController {

    private final AdminService adminService;
    private final DepartmentService departmentService;
    private final AuthService authService;
    private final UserRepository userRepository;

    public AdminController(AdminService adminService,
                           DepartmentService departmentService,
                           AuthService authService,
                           UserRepository userRepository) {
        this.adminService = adminService;
        this.departmentService = departmentService;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    // Admin doctor management (FR-ADM-01, FR-ADM-04, FR-ADM-05)
    @GetMapping("/admin/doctors")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all doctors (active & inactive)", description = "Admin view of all clinicians")
    public ResponseEntity<List<DoctorResponse>> getAllDoctors() {
        return ResponseEntity.ok(adminService.getAllDoctors());
    }

    @PostMapping("/admin/doctors")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create doctor profile", description = "Registers clinician, staff ID, and generates slots (FR-ADM-01)")
    public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        DoctorResponse res = adminService.createDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PostMapping("/admin/doctors/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate doctor profile", description = "Hides from search while keeping history (FR-ADM-03)")
    public ResponseEntity<DoctorResponse> deactivateDoctor(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.deactivateDoctor(id));
    }

    @PostMapping("/admin/doctors/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivate doctor profile", description = "Re-publishes doctor to search")
    public ResponseEntity<DoctorResponse> activateDoctor(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.activateDoctor(id));
    }

    // Department management (FR-ADM-02)
    @PostMapping("/admin/departments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create department", description = "Add department master record")
    public ResponseEntity<DepartmentDto> createDepartment(@Valid @RequestBody DepartmentDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.createDepartment(dto));
    }

    @PutMapping("/admin/departments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update department hours or info", description = "Constrains doctor hours validation (FR-ADM-02)")
    public ResponseEntity<DepartmentDto> updateDepartment(@PathVariable UUID id, @Valid @RequestBody DepartmentDto dto) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, dto));
    }

    // Reports (FR-RPT-01, FR-RPT-02, FR-RPT-03)
    @GetMapping("/admin/reports")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Appointment & no-show statistics", description = "Counts by department, status, and no-show rate (FR-RPT-01)")
    public ResponseEntity<ReportDto> getReports(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return ResponseEntity.ok(adminService.generateReport(startDate, endDate));
    }

    @GetMapping(value = "/admin/reports/csv", produces = "text/csv")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Export reports as CSV", description = "Download appointment report in CSV format (FR-RPT-03)")
    public ResponseEntity<String> exportReportCsv(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        String csv = adminService.exportReportCsv(startDate, endDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"hoabs-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/admin/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Audit logs", description = "Audit trail of overrides and critical security events (FR-ADM-08)")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(adminService.getAuditLogs());
    }

    // Receptionist walk-in registration & search (FR-ADM-06)
    @PostMapping("/reception/patients")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    @Operation(summary = "Register walk-in patient", description = "Reception creates patient account on their behalf (FR-ADM-06)")
    public ResponseEntity<AuthResponse> registerWalkInPatient(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @GetMapping("/reception/patients/search")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    @Operation(summary = "Search patient by phone/email/name", description = "Reception searches for patient to book on their behalf")
    public ResponseEntity<List<UserResponse>> searchPatients(@RequestParam String query) {
        List<UserResponse> list = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.PATIENT)
                .filter(u -> u.getPhone().contains(query)
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(query.toLowerCase()))
                        || u.getFullName().toLowerCase().contains(query.toLowerCase()))
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }
}
