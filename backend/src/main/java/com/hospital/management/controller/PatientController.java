package com.hospital.management.controller;

import com.hospital.management.dto.AppointmentRequest;
import com.hospital.management.model.Appointment;
import com.hospital.management.model.Department;
import com.hospital.management.model.Doctor;
import com.hospital.management.model.MedicalRecord;
import com.hospital.management.model.Patient;
import com.hospital.management.model.Prescription;
import com.hospital.management.service.AppointmentService;
import com.hospital.management.service.DoctorService;
import com.hospital.management.service.MedicalRecordService;
import com.hospital.management.service.PatientService;
import com.hospital.management.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient")
@CrossOrigin(origins = "http://localhost:3000")
public class PatientController {

    @Autowired
    private PatientService patientService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private PrescriptionService prescriptionService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Patient> getPatientProfile(@RequestParam Long userId) {
        Patient patient = patientService.getPatientByUserId(userId);
        return ResponseEntity.ok(patient);
    }

    @GetMapping("/departments")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<Department>> getActiveDepartments() {
        return ResponseEntity.ok(doctorService.getActiveDepartments());
    }

    @GetMapping("/doctors/available")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<Doctor>> getAvailableDoctors(
            @RequestParam(required = false) Long departmentId) {
        if (departmentId == null) {
            return ResponseEntity.ok(doctorService.getAvailableDoctors());
        }
        return ResponseEntity.ok(doctorService.getAvailableDoctors(departmentId));
    }

    @GetMapping("/appointments")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<Appointment>> getMyAppointments(@RequestParam Long patientId) {
        List<Appointment> appointments = appointmentService.getPatientAppointments(patientId);
        return ResponseEntity.ok(appointments);
    }

    @PostMapping("/appointments")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Appointment> bookAppointment(@RequestParam Long patientId, @Valid @RequestBody AppointmentRequest request) {
        Appointment appointment = appointmentService.bookAppointment(patientId, request);
        return ResponseEntity.ok(appointment);
    }

    @DeleteMapping("/appointments/{appointmentId}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<String> cancelAppointment(@PathVariable Long appointmentId) {
        appointmentService.cancelAppointment(appointmentId);
        return ResponseEntity.ok("Appointment cancelled successfully");
    }

    @GetMapping("/medical-history")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<MedicalRecord>> getMedicalHistory(@RequestParam Long patientId) {
        List<MedicalRecord> medicalRecords = medicalRecordService.getPatientMedicalHistory(patientId);
        return ResponseEntity.ok(medicalRecords);
    }

    @GetMapping("/prescriptions")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<Prescription>> getMyPrescriptions(@RequestParam Long patientId) {
        List<Prescription> prescriptions = prescriptionService.getPatientPrescriptions(patientId);
        return ResponseEntity.ok(prescriptions);
    }
}