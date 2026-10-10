package rw.ac.rca.hoabs.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.rca.hoabs.appointment.Appointment;
import rw.ac.rca.hoabs.appointment.AppointmentRepository;
import rw.ac.rca.hoabs.appointment.AppointmentStatus;
import rw.ac.rca.hoabs.department.Department;
import rw.ac.rca.hoabs.department.DepartmentRepository;
import rw.ac.rca.hoabs.doctor.*;
import rw.ac.rca.hoabs.identity.Role;
import rw.ac.rca.hoabs.identity.User;
import rw.ac.rca.hoabs.identity.UserRepository;
import rw.ac.rca.hoabs.identity.UserStatus;
import rw.ac.rca.hoabs.slot.Slot;
import rw.ac.rca.hoabs.slot.SlotGeneratorService;
import rw.ac.rca.hoabs.slot.SlotRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final LeaveBlockRepository leaveBlockRepository;
    private final SlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final SlotGeneratorService slotGeneratorService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           DepartmentRepository departmentRepository,
                           DoctorRepository doctorRepository,
                           WorkingHoursRepository workingHoursRepository,
                           LeaveBlockRepository leaveBlockRepository,
                           SlotRepository slotRepository,
                           AppointmentRepository appointmentRepository,
                           SlotGeneratorService slotGeneratorService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
        this.workingHoursRepository = workingHoursRepository;
        this.leaveBlockRepository = leaveBlockRepository;
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.slotGeneratorService = slotGeneratorService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded with demo data. Skipping DataInitializer.");
            return;
        }

        log.info("Seeding HOABS initial data according to SRS Appendix B...");

        String defaultHash = passwordEncoder.encode("Password1");

        // 1. Admin
        User admin = new User();
        admin.setFullName("Hospital Admin");
        admin.setPhone("0788000000");
        admin.setEmail("admin@example.com");
        admin.setPasswordHash(defaultHash);
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        userRepository.save(admin);

        // 2. Patient A (Happy path booking)
        User patientA = new User();
        patientA.setFullName("Patient Alpha");
        patientA.setPhone("0788000001");
        patientA.setEmail("patient.a@example.com");
        patientA.setPasswordHash(defaultHash);
        patientA.setRole(Role.PATIENT);
        patientA.setStatus(UserStatus.ACTIVE);
        patientA.setDateOfBirth(LocalDate.of(1995, 5, 12));
        userRepository.save(patientA);

        // 3. Patient B (Has 3 upcoming Confirmed appointments to test booking-limit)
        User patientB = new User();
        patientB.setFullName("Patient Beta");
        patientB.setPhone("0788000002");
        patientB.setEmail("patient.b@example.com");
        patientB.setPasswordHash(defaultHash);
        patientB.setRole(Role.PATIENT);
        patientB.setStatus(UserStatus.ACTIVE);
        patientB.setDateOfBirth(LocalDate.of(1988, 11, 20));
        userRepository.save(patientB);

        // 4. Locked Patient (AU-005)
        User lockedPatient = new User();
        lockedPatient.setFullName("Locked Patient");
        lockedPatient.setPhone("0788000003");
        lockedPatient.setEmail("locked@example.com");
        lockedPatient.setPasswordHash(defaultHash);
        lockedPatient.setRole(Role.PATIENT);
        lockedPatient.setStatus(UserStatus.LOCKED);
        lockedPatient.setFailedAttempts(3);
        lockedPatient.setLockedUntil(Instant.now().plusSeconds(1800)); // 30 minutes in future
        userRepository.save(lockedPatient);

        // 5. Departments (Opening hours 07:00 - 17:00 Mon-Sat)
        Department cardio = new Department();
        cardio.setName("Cardiology");
        cardio.setDescription("Heart, blood vessels, and cardiovascular consultations.");
        cardio.setOpeningTime(LocalTime.of(7, 0));
        cardio.setClosingTime(LocalTime.of(17, 0));
        departmentRepository.save(cardio);

        Department paeds = new Department();
        paeds.setName("Paediatrics");
        paeds.setDescription("Medical care for infants, children, and adolescents.");
        paeds.setOpeningTime(LocalTime.of(7, 0));
        paeds.setClosingTime(LocalTime.of(17, 0));
        departmentRepository.save(paeds);

        Department genMed = new Department();
        genMed.setName("General Medicine");
        genMed.setDescription("Comprehensive primary care and outpatient general medical checkups.");
        genMed.setOpeningTime(LocalTime.of(7, 0));
        genMed.setClosingTime(LocalTime.of(17, 0));
        departmentRepository.save(genMed);

        // 6. Doctor DOC-001: Dr. Patrick Mugisha (Cardiology, Mon-Fri 08:00-16:00, leave next week)
        User docUser1 = new User();
        docUser1.setFullName("Dr. Patrick Mugisha");
        docUser1.setPhone("0788000101");
        docUser1.setEmail("doc1@amahoro.gov.rw");
        docUser1.setPasswordHash(defaultHash);
        docUser1.setRole(Role.DOCTOR);
        docUser1.setStatus(UserStatus.ACTIVE);
        userRepository.save(docUser1);

        Doctor doc1 = new Doctor();
        doc1.setUser(docUser1);
        doc1.setStaffId("DOC-001");
        doc1.setDepartment(cardio);
        doc1.setSpecialty("Cardiology");
        doc1.setSlotDurationMinutes(20);
        doc1.setConsultationFeeRwf(0);
        doc1.setActive(true);
        doctorRepository.save(doc1);

        for (int day = 1; day <= 5; day++) {
            WorkingHours wh = new WorkingHours();
            wh.setDoctor(doc1);
            wh.setDayOfWeek(day);
            wh.setStartTime(LocalTime.of(8, 0));
            wh.setEndTime(LocalTime.of(16, 0));
            workingHoursRepository.save(wh);
        }

        // Leave block for DOC-001 next week
        LeaveBlock leave = new LeaveBlock();
        leave.setDoctor(doc1);
        leave.setStartDate(LocalDate.now().plusDays(7));
        leave.setEndDate(LocalDate.now().plusDays(8));
        leave.setReason("Annual Cardiology CME Conference");
        leaveBlockRepository.save(leave);

        // 7. Doctor DOC-002: Dr. Aline Uwase (Paediatrics, inactive - must NOT appear in search)
        User docUser2 = new User();
        docUser2.setFullName("Dr. Aline Uwase");
        docUser2.setPhone("0788000102");
        docUser2.setEmail("doc2@amahoro.gov.rw");
        docUser2.setPasswordHash(defaultHash);
        docUser2.setRole(Role.DOCTOR);
        docUser2.setStatus(UserStatus.ACTIVE);
        userRepository.save(docUser2);

        Doctor doc2 = new Doctor();
        doc2.setUser(docUser2);
        doc2.setStaffId("DOC-002");
        doc2.setDepartment(paeds);
        doc2.setSpecialty("Paediatrics");
        doc2.setSlotDurationMinutes(20);
        doc2.setConsultationFeeRwf(0);
        doc2.setActive(false); // Inactive
        doctorRepository.save(doc2);

        // 8. Doctor DOC-003: Dr. Jean Claude Niyonzima (General Medicine, Active, Mon-Fri 08:00-16:00)
        User docUser3 = new User();
        docUser3.setFullName("Dr. Jean Claude Niyonzima");
        docUser3.setPhone("0788000103");
        docUser3.setEmail("doc3@amahoro.gov.rw");
        docUser3.setPasswordHash(defaultHash);
        docUser3.setRole(Role.DOCTOR);
        docUser3.setStatus(UserStatus.ACTIVE);
        userRepository.save(docUser3);

        Doctor doc3 = new Doctor();
        doc3.setUser(docUser3);
        doc3.setStaffId("DOC-003");
        doc3.setDepartment(genMed);
        doc3.setSpecialty("General Medicine");
        doc3.setSlotDurationMinutes(20);
        doc3.setConsultationFeeRwf(0);
        doc3.setActive(true);
        doctorRepository.save(doc3);

        for (int day = 1; day <= 5; day++) {
            WorkingHours wh = new WorkingHours();
            wh.setDoctor(doc3);
            wh.setDayOfWeek(day);
            wh.setStartTime(LocalTime.of(8, 0));
            wh.setEndTime(LocalTime.of(16, 0));
            workingHoursRepository.save(wh);
        }

        // 9. Generate open slots for DOC-001 and DOC-003
        slotGeneratorService.generateSlotsForDoctor(doc1, 14);
        slotGeneratorService.generateSlotsForDoctor(doc3, 14);

        // 10. Seed 3 upcoming Confirmed appointments for Patient B to test booking limit rule
        List<Slot> futureSlots = slotRepository.findFutureSlotsForDoctor(doc1.getId(), Instant.now().plusSeconds(7200));
        if (futureSlots.size() >= 3) {
            for (int i = 0; i < 3; i++) {
                Slot s = futureSlots.get(i);
                s.setIsOpen(false);
                slotRepository.save(s);

                Appointment appt = new Appointment();
                appt.setReferenceNo("APT-BETA-00" + (i + 1));
                appt.setPatient(patientB);
                appt.setDoctor(doc1);
                appt.setSlot(s);
                appt.setReason("Routine follow-up session " + (i + 1));
                appt.setStatus(AppointmentStatus.CONFIRMED);
                appointmentRepository.save(appt);
            }
        }

        log.info("HOABS seed data loaded successfully: Admin, Patient A, Patient B (3 bookings), Locked Patient, DOC-001, DOC-002 (inactive), DOC-003, and slots.");
    }
}
