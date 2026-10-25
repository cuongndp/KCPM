package com.hospital.management.controller;

import com.hospital.management.dto.MedicalRecordRequest;
import com.hospital.management.dto.PrescriptionRequest;
import com.hospital.management.model.Appointment;
import com.hospital.management.model.Doctor;
import com.hospital.management.model.MedicalRecord;
import com.hospital.management.model.Prescription;
import com.hospital.management.service.AppointmentService;
import com.hospital.management.service.MedicalRecordService;
import com.hospital.management.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
@CrossOrigin(origins = "http://localhost:3000")
public class DoctorController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private com.hospital.management.service.DoctorService doctorService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Doctor> getDoctorProfile(@RequestParam Long userId) {
        return ResponseEntity.ok(doctorService.getDoctorByUserId(userId));
    }

    @GetMapping("/appointments")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<Appointment>> getMyAppointments(@RequestParam Long doctorId) {
        List<Appointment> appointments = appointmentService.getDoctorAppointments(doctorId);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/appointments/upcoming")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<Appointment>> getUpcomingAppointments(@RequestParam Long doctorId) {
        List<Appointment> appointments = appointmentService.getUpcomingAppointments(doctorId);
        return ResponseEntity.ok(appointments);
    }

    @PutMapping("/appointments/{appointmentId}/status")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Appointment> updateAppointmentStatus(
            @PathVariable Long appointmentId,
            @RequestParam Appointment.Status status) {
        Appointment appointment = appointmentService.updateAppointmentStatus(appointmentId, status);
        return ResponseEntity.ok(appointment);
    }

    @PostMapping("/medical-records")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<MedicalRecord> createMedicalRecord(
            @RequestParam Long doctorId,
            @Valid @RequestBody MedicalRecordRequest request) {
        MedicalRecord medicalRecord = medicalRecordService.createMedicalRecord(doctorId, request);
        return ResponseEntity.ok(medicalRecord);
    }

    @GetMapping("/medical-records")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<MedicalRecord>> getMyMedicalRecords(@RequestParam Long doctorId) {
        List<MedicalRecord> medicalRecords = medicalRecordService.getDoctorMedicalRecords(doctorId);
        return ResponseEntity.ok(medicalRecords);
    }

    @PostMapping("/prescriptions")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Prescription> createPrescription(
            @RequestParam Long doctorId,
            @Valid @RequestBody PrescriptionRequest request) {
        Prescription prescription = prescriptionService.createPrescription(doctorId, request);
        return ResponseEntity.ok(prescription);
    }

    @GetMapping("/prescriptions")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<Prescription>> getMyPrescriptions(@RequestParam Long doctorId) {
        List<Prescription> prescriptions = prescriptionService.getDoctorPrescriptions(doctorId);
        return ResponseEntity.ok(prescriptions);
    }
}