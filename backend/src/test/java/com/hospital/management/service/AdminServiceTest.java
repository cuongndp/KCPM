package com.hospital.management.service;

import com.hospital.management.dto.CreateDoctorRequest;
import com.hospital.management.exception.ResourceNotFoundException;
import com.hospital.management.model.Department;
import com.hospital.management.model.Doctor;
import com.hospital.management.model.User;
import com.hospital.management.repository.DepartmentRepository;
import com.hospital.management.repository.DoctorRepository;
import com.hospital.management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminService adminService;

    private CreateDoctorRequest request;
    private User validDoctorUser;
    private Department validDepartment;

    @BeforeEach
    void setUp() {
        request = new CreateDoctorRequest();
        request.setUserId(5L);
        request.setSpecialization("Y học tổng quát");
        request.setQualification("Bác sĩ đa khoa");
        request.setExperience(2);

        validDoctorUser = new User();
        validDoctorUser.setId(5L);
        validDoctorUser.setUsername("doctor5");
        validDoctorUser.setRole(User.Role.DOCTOR);

        validDepartment = new Department();
        validDepartment.setId(1L);
        validDepartment.setName("Y học tổng quát");
    }

    @Test
    @DisplayName("Path 1 (Basis Path 1): Ném ResourceNotFoundException khi không tìm thấy User theo userId")
    void testCreateDoctor_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            adminService.createDoctorWithDepartment(request, 1L);
        });

        assertTrue(exception.getMessage().contains("User"));
        verify(userRepository, times(1)).findById(5L);
        verifyNoInteractions(doctorRepository);
        verifyNoInteractions(departmentRepository);
    }

    @Test
    @DisplayName("Path 2 (Basis Path 2): Ném ResponseStatusException (400 BAD_REQUEST) khi User không có vai trò DOCTOR")
    void testCreateDoctor_UserRoleNotDoctor_ThrowsBadRequest() {
        // Arrange: User có role PATIENT
        User patientUser = new User();
        patientUser.setId(5L);
        patientUser.setRole(User.Role.PATIENT);

        when(userRepository.findById(5L)).thenReturn(Optional.of(patientUser));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            adminService.createDoctorWithDepartment(request, 1L);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("không có vai trò bác sĩ"));
        verify(userRepository, times(1)).findById(5L);
        verify(doctorRepository, never()).existsByUserId(anyLong());
    }

    @Test
    @DisplayName("Path 3 (Basis Path 3): Ném ResponseStatusException (409 CONFLICT) khi User đã có hồ sơ bác sĩ")
    void testCreateDoctor_DoctorProfileAlreadyExists_ThrowsConflict() {
        // Arrange: User hợp lệ nhưng đã tồn tại trong doctorRepository
        when(userRepository.findById(5L)).thenReturn(Optional.of(validDoctorUser));
        when(doctorRepository.existsByUserId(5L)).thenReturn(true);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            adminService.createDoctorWithDepartment(request, 1L);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("đã có hồ sơ bác sĩ"));
        verify(userRepository, times(1)).findById(5L);
        verify(doctorRepository, times(1)).existsByUserId(5L);
        verify(departmentRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Path 4 (Basis Path 4): Ném ResourceNotFoundException khi không tìm thấy Khoa theo departmentId")
    void testCreateDoctor_DepartmentNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(5L)).thenReturn(Optional.of(validDoctorUser));
        when(doctorRepository.existsByUserId(5L)).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            adminService.createDoctorWithDepartment(request, 1L);
        });

        assertTrue(exception.getMessage().contains("Department"));
        verify(departmentRepository, times(1)).findById(1L);
        verify(doctorRepository, never()).save(any(Doctor.class));
    }

    @Test
    @DisplayName("Path 5 (Basis Path 5 - Thành công): Tạo hồ sơ bác sĩ thành công khi thỏa mãn toàn bộ điều kiện")
    void testCreateDoctor_Success() {
        // Arrange
        Doctor savedDoctor = new Doctor();
        savedDoctor.setId(10L);
        savedDoctor.setUser(validDoctorUser);
        savedDoctor.setDepartment(validDepartment);
        savedDoctor.setSpecialization("Y học tổng quát");
        savedDoctor.setQualification("Bác sĩ đa khoa");
        savedDoctor.setExperience(2);
        savedDoctor.setAvailable(true);

        when(userRepository.findById(5L)).thenReturn(Optional.of(validDoctorUser));
        when(doctorRepository.existsByUserId(5L)).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(validDepartment));
        when(doctorRepository.save(any(Doctor.class))).thenReturn(savedDoctor);

        // Act
        Doctor result = adminService.createDoctorWithDepartment(request, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(validDoctorUser, result.getUser());
        assertEquals(validDepartment, result.getDepartment());
        assertEquals("Y học tổng quát", result.getSpecialization());
        assertEquals("Bác sĩ đa khoa", result.getQualification());
        assertEquals(2, result.getExperience());
        assertTrue(result.getAvailable());

        verify(userRepository, times(1)).findById(5L);
        verify(doctorRepository, times(1)).existsByUserId(5L);
        verify(departmentRepository, times(1)).findById(1L);
        verify(doctorRepository, times(1)).save(any(Doctor.class));
    }
}
