package rw.ac.rca.hoabs.admin;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.rca.hoabs.appointment.Appointment;
import rw.ac.rca.hoabs.appointment.AppointmentRepository;
import rw.ac.rca.hoabs.appointment.AppointmentStatus;
import rw.ac.rca.hoabs.common.exception.ApiException;
import rw.ac.rca.hoabs.department.Department;
import rw.ac.rca.hoabs.department.DepartmentRepository;
import rw.ac.rca.hoabs.doctor.Doctor;
import rw.ac.rca.hoabs.doctor.DoctorRepository;
import rw.ac.rca.hoabs.doctor.DoctorResponse;
import rw.ac.rca.hoabs.identity.*;
import rw.ac.rca.hoabs.slot.SlotGeneratorService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppointmentRepository appointmentRepository;
    private final SlotGeneratorService slotGeneratorService;
    private final AuditLogRepository auditLogRepository;

    public AdminService(DoctorRepository doctorRepository,
                        DepartmentRepository departmentRepository,
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        AppointmentRepository appointmentRepository,
                        SlotGeneratorService slotGeneratorService,
                        AuditLogRepository auditLogRepository) {
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.appointmentRepository = appointmentRepository;
        this.slotGeneratorService = slotGeneratorService;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public DoctorResponse createDoctor(CreateDoctorRequest req) {
        // FR-ADM-04 / UAT AD-002: Duplicate staff identifier rejected
        if (doctorRepository.existsByStaffId(req.staffId())) {
            throw ApiException.conflict("Doctor with staff ID '" + req.staffId() + "' already exists.");
        }

        if (userRepository.existsByPhone(req.phone())) {
            throw ApiException.conflict("Phone number '" + req.phone() + "' already registered.");
        }

        Department dept = departmentRepository.findById(req.departmentId())
                .orElseThrow(() -> ApiException.notFound("Department not found with id: " + req.departmentId()));

        User user = new User();
        user.setFullName(req.fullName().trim());
        user.setPhone(req.phone().trim());
        user.setEmail(req.email() != null ? req.email().trim().toLowerCase() : null);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(Role.DOCTOR);
        user.setStatus(UserStatus.ACTIVE);
        User savedUser = userRepository.save(user);

        Doctor doctor = new Doctor();
        doctor.setUser(savedUser);
        doctor.setStaffId(req.staffId().trim());
        doctor.setDepartment(dept);
        doctor.setSpecialty(req.specialty().trim());
        doctor.setSlotDurationMinutes(req.slotDurationMinutes() != null ? req.slotDurationMinutes() : 20);
        doctor.setConsultationFeeRwf(req.consultationFeeRwf() != null ? req.consultationFeeRwf() : 0);
        doctor.setActive(true);

        Doctor savedDoctor = doctorRepository.save(doctor);
        slotGeneratorService.generateSlotsForDoctor(savedDoctor, 14);

        return DoctorResponse.from(savedDoctor, null);
    }

    @Transactional
    public DoctorResponse deactivateDoctor(UUID doctorId) {
        Doctor doc = doctorRepository.findById(doctorId)
                .orElseThrow(() -> ApiException.notFound("Doctor not found with id: " + doctorId));
        // FR-ADM-03 / UAT AD-003: Deactivating hides doctor from search while preserving history
        doc.setActive(false);
        Doctor saved = doctorRepository.save(doc);
        return DoctorResponse.from(saved, null);
    }

    @Transactional
    public DoctorResponse activateDoctor(UUID doctorId) {
        Doctor doc = doctorRepository.findById(doctorId)
                .orElseThrow(() -> ApiException.notFound("Doctor not found with id: " + doctorId));
        doc.setActive(true);
        Doctor saved = doctorRepository.save(doc);
        slotGeneratorService.generateSlotsForDoctor(saved, 14);
        return DoctorResponse.from(saved, null);
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> getAllDoctors() {
        return doctorRepository.findAll().stream()
                .map(d -> DoctorResponse.from(d, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportDto generateReport(LocalDate startDate, LocalDate endDate) {
        LocalDate start = (startDate != null) ? startDate : LocalDate.now(SlotGeneratorService.HOSPITAL_ZONE).minusDays(30);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now(SlotGeneratorService.HOSPITAL_ZONE).plusDays(1);

        Instant from = start.atStartOfDay(SlotGeneratorService.HOSPITAL_ZONE).toInstant();
        Instant to = end.atStartOfDay(SlotGeneratorService.HOSPITAL_ZONE).toInstant();

        List<Appointment> list = appointmentRepository.findAllInDateRange(from, to);

        long confirmed = list.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count();
        long completed = list.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
        long cancelled = list.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();
        long noShow = list.stream().filter(a -> a.getStatus() == AppointmentStatus.NO_SHOW).count();
        long total = list.size();

        double overallNoShowRate = (completed + noShow > 0) ? ((double) noShow / (completed + noShow)) * 100.0 : 0.0;

        // Breakdown by department
        Map<String, Map<String, Long>> deptCounts = new HashMap<>();
        Map<String, Double> deptNoShowRates = new HashMap<>();

        Map<String, List<Appointment>> byDept = list.stream()
                .collect(Collectors.groupingBy(a -> a.getDoctor().getDepartment().getName()));

        for (Map.Entry<String, List<Appointment>> entry : byDept.entrySet()) {
            String deptName = entry.getKey();
            List<Appointment> deptAppts = entry.getValue();

            Map<String, Long> statusCounts = new HashMap<>();
            long deptCompleted = deptAppts.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
            long deptNoShow = deptAppts.stream().filter(a -> a.getStatus() == AppointmentStatus.NO_SHOW).count();

            statusCounts.put("CONFIRMED", deptAppts.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count());
            statusCounts.put("COMPLETED", deptCompleted);
            statusCounts.put("CANCELLED", deptAppts.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count());
            statusCounts.put("NO_SHOW", deptNoShow);
            deptCounts.put(deptName, statusCounts);

            double deptRate = (deptCompleted + deptNoShow > 0) ? ((double) deptNoShow / (deptCompleted + deptNoShow)) * 100.0 : 0.0;
            deptNoShowRates.put(deptName, Math.round(deptRate * 10.0) / 10.0);
        }

        // Breakdown by doctor
        Map<String, Double> docNoShowRates = new HashMap<>();
        Map<String, List<Appointment>> byDoctor = list.stream()
                .collect(Collectors.groupingBy(a -> a.getDoctor().getUser().getFullName()));

        for (Map.Entry<String, List<Appointment>> entry : byDoctor.entrySet()) {
            String docName = entry.getKey();
            List<Appointment> docAppts = entry.getValue();
            long docCompleted = docAppts.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
            long docNoShow = docAppts.stream().filter(a -> a.getStatus() == AppointmentStatus.NO_SHOW).count();
            double docRate = (docCompleted + docNoShow > 0) ? ((double) docNoShow / (docCompleted + docNoShow)) * 100.0 : 0.0;
            docNoShowRates.put(docName, Math.round(docRate * 10.0) / 10.0);
        }

        return new ReportDto(
                total, confirmed, completed, cancelled, noShow,
                Math.round(overallNoShowRate * 10.0) / 10.0,
                deptCounts, docNoShowRates, deptNoShowRates
        );
    }

    @Transactional(readOnly = true)
    public String exportReportCsv(LocalDate startDate, LocalDate endDate) {
        ReportDto report = generateReport(startDate, endDate);
        StringBuilder sb = new StringBuilder();
        sb.append("Department,Status,Count\n");
        for (Map.Entry<String, Map<String, Long>> entry : report.departmentCounts().entrySet()) {
            String dept = entry.getKey();
            for (Map.Entry<String, Long> statusEntry : entry.getValue().entrySet()) {
                sb.append(String.format("\"%s\",%s,%d\n", dept, statusEntry.getKey(), statusEntry.getValue()));
            }
        }
        sb.append("\nDoctor,NoShowRatePercent\n");
        for (Map.Entry<String, Double> entry : report.doctorNoShowRates().entrySet()) {
            sb.append(String.format("\"%s\",%.1f%%\n", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }
}
