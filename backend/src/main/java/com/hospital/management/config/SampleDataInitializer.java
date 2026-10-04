package com.hospital.management.config;

import com.hospital.management.model.Department;
import com.hospital.management.model.Doctor;
import com.hospital.management.model.Patient;
import com.hospital.management.model.User;
import com.hospital.management.repository.DepartmentRepository;
import com.hospital.management.repository.DoctorRepository;
import com.hospital.management.repository.PatientRepository;
import com.hospital.management.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class SampleDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public SampleDataInitializer(
            UserRepository userRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureUser("1", "1", "sample-admin@hospital.test",
                "Admin", "Demo", User.Role.ADMIN);
        User doctorUser = ensureUser("2", "2", "sample-doctor@hospital.test",
                "Doctor", "Demo", User.Role.DOCTOR);
        User patientUser = ensureUser("3", "3", "sample-patient@hospital.test",
                "Patient", "Demo", User.Role.PATIENT);

        ensureDoctorProfile(doctorUser);
        ensurePatientProfile(patientUser);
    }

    private User ensureUser(
            String username,
            String password,
            String email,
            String firstName,
            String lastName,
            User.Role role) {
        User user = userRepository.findByUsername(username).orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setPhoneNumber("0900000000");
            newUser.setRole(role);
            newUser.setActive(true);
            return newUser;
        });

        if (user.getRole() != role) {
            throw new IllegalStateException(
                    "Sample username '" + username + "' is already assigned to role " + user.getRole());
        }

        user.setPassword(passwordEncoder.encode(password));
        user.setActive(true);
        return userRepository.save(user);
    }

    private void ensureDoctorProfile(User user) {
        if (doctorRepository.findByUserId(user.getId()).isPresent()) {
            return;
        }

        Department department = departmentRepository.findByName("Y học tổng quát")
                .orElseThrow(() -> new IllegalStateException(
                        "Required sample department 'Y học tổng quát' was not initialized"));

        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setDepartment(department);
        doctor.setSpecialization("Y học tổng quát");
        doctor.setQualification("Bác sĩ");
        doctor.setExperience(5);
        doctor.setAvailable(true);
        doctor.setConsultationFee(0.0);
        doctorRepository.save(doctor);
    }

    private void ensurePatientProfile(User user) {
        if (patientRepository.findByUserId(user.getId()).isPresent()) {
            return;
        }

        Patient patient = new Patient();
        patient.setUser(user);
        patient.setAddress("Địa chỉ mẫu");
        patient.setBloodGroup("O+");
        patient.setEmergencyContact("0900000000");
        patientRepository.save(patient);
    }
}
