package com.hospital.management.service;

import com.hospital.management.exception.ResourceNotFoundException;
import com.hospital.management.model.Department;
import com.hospital.management.model.Doctor;
import com.hospital.management.repository.AppointmentRepository;
import com.hospital.management.repository.DepartmentRepository;
import com.hospital.management.repository.DoctorRepository;
import com.hospital.management.repository.MedicalRecordRepository;
import com.hospital.management.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    public List<Doctor> getAvailableDoctors() {
        return doctorRepository.findByAvailableTrue();
    }

    public List<Department> getActiveDepartments() {
        return departmentRepository.findByActiveTrue();
    }

    public List<Doctor> getAvailableDoctors(Long departmentId) {
        return doctorRepository.findByDepartmentIdAndDepartmentActiveTrueAndAvailableTrue(departmentId);
    }

    public Doctor getDoctorById(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));
    }

    public Doctor getDoctorByUserId(Long userId) {
        return doctorRepository.findByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Tài khoản bác sĩ chưa được quản trị viên cấp hồ sơ và quyền truy cập. Vui lòng liên hệ quản trị viên."));
    }

    public void deleteDoctor(Long doctorId) {
        Doctor doctor = getDoctorById(doctorId);
        if (appointmentRepository.existsByDoctorId(doctorId)
                || medicalRecordRepository.existsByDoctorId(doctorId)
                || prescriptionRepository.existsByDoctorId(doctorId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể xóa bác sĩ đã có cuộc hẹn, hồ sơ y tế hoặc đơn thuốc.");
        }
        doctorRepository.delete(doctor);
    }

    public Doctor updateAvailability(Long doctorId, Boolean available) {
        Doctor doctor = getDoctorById(doctorId);
        doctor.setAvailable(available);
        return doctorRepository.save(doctor);
    }
}