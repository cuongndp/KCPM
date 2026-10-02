package com.hospital.management.service;

import com.hospital.management.dto.CreateDoctorRequest;
import com.hospital.management.dto.DoctorCandidateResponse;
import com.hospital.management.exception.ResourceNotFoundException;
import com.hospital.management.model.Department;
import com.hospital.management.model.Doctor;
import com.hospital.management.model.User;
import com.hospital.management.repository.DepartmentRepository;
import com.hospital.management.repository.DoctorRepository;
import com.hospital.management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class AdminService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Department getDepartmentById(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", departmentId));
    }

    public Department createDepartment(Department department) {
        if (departmentRepository.existsByName(department.getName())) {
            throw new RuntimeException("Department with this name already exists");
        }
        return departmentRepository.save(department);
    }

    public void deleteDepartment(Long departmentId) {
        Department department = getDepartmentById(departmentId);
        departmentRepository.delete(department);
    }

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public List<DoctorCandidateResponse> getDoctorCandidates() {
        return userRepository.findUsersWithoutDoctorProfile(User.Role.DOCTOR).stream()
                .map(user -> new DoctorCandidateResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail()))
                .toList();
    }

    public Doctor createDoctorWithDepartment(CreateDoctorRequest request, Long departmentId) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));
        if (user.getRole() != User.Role.DOCTOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản được chọn không có vai trò bác sĩ.");
        }
        if (doctorRepository.existsByUserId(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tài khoản này đã có hồ sơ bác sĩ.");
        }

        Department department = getDepartmentById(departmentId);
        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setDepartment(department);
        doctor.setSpecialization(request.getSpecialization());
        doctor.setQualification(request.getQualification());
        doctor.setExperience(request.getExperience());
        doctor.setConsultationFee(0.0);
        doctor.setAvailable(true);
        return doctorRepository.save(doctor);
    }

}