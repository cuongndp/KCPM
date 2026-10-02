package com.hospital.management.controller;

import com.hospital.management.dto.DoctorAvailabilityRequest;
import com.hospital.management.dto.CreateDoctorRequest;
import com.hospital.management.dto.DoctorCandidateResponse;
import com.hospital.management.model.Department;
import com.hospital.management.model.Doctor;
import com.hospital.management.service.AdminService;
import com.hospital.management.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private DoctorService doctorService;

    @GetMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Department>> getAllDepartments() {
        List<Department> departments = adminService.getAllDepartments();
        return ResponseEntity.ok(departments);
    }

    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> createDepartment(@RequestBody Department department) {
        Department createdDepartment = adminService.createDepartment(department);
        return ResponseEntity.ok(createdDepartment);
    }

    @DeleteMapping("/departments/{departmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteDepartment(@PathVariable Long departmentId) {
        adminService.deleteDepartment(departmentId);
        return ResponseEntity.ok("Department deleted successfully");
    }

    @GetMapping("/doctors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Doctor>> getAllDoctors() {
        return ResponseEntity.ok(adminService.getAllDoctors());
    }

    @GetMapping("/users/doctor-candidates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DoctorCandidateResponse>> getDoctorCandidates() {
        return ResponseEntity.ok(adminService.getDoctorCandidates());
    }

    @PostMapping("/doctors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Doctor> createDoctor(
            @Valid @RequestBody CreateDoctorRequest request,
            @RequestParam Long departmentId) {
        Doctor createdDoctor = adminService.createDoctorWithDepartment(request, departmentId);
        return ResponseEntity.ok(createdDoctor);
    }

    @DeleteMapping("/doctors/{doctorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteDoctor(@PathVariable Long doctorId) {
        doctorService.deleteDoctor(doctorId);
        return ResponseEntity.ok("Doctor deleted successfully");
    }

    @PatchMapping("/doctors/{doctorId}/availability")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Doctor> updateDoctorAvailability(
            @PathVariable Long doctorId,
            @Valid @RequestBody DoctorAvailabilityRequest request) {
        Doctor doctor = doctorService.updateAvailability(doctorId, request.getAvailable());
        return ResponseEntity.ok(doctor);
    }
}