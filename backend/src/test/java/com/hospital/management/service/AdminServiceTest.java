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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    private static final Long USER_ID = 3L;
    private static final Long DEPARTMENT_ID = 1L;

    @Mock private DepartmentRepository departmentRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private AdminService service;

    private CreateDoctorRequest request;
    private User doctorUser;
    private Department department;

    @BeforeEach
    void setUp() {
        request = new CreateDoctorRequest(USER_ID, "Tim mạch", "Bác sĩ chuyên khoa I", 5);

        doctorUser = new User();
        doctorUser.setId(USER_ID);
        doctorUser.setUsername("bacsi3");
        doctorUser.setEmail("bacsi3@hospital.vn");
        doctorUser.setFirstName("An");
        doctorUser.setLastName("Nguyễn");
        doctorUser.setRole(User.Role.DOCTOR);

        department = new Department();
        department.setId(DEPARTMENT_ID);
        department.setName("Khoa Nội");
    }

    @Test
    @DisplayName("W1 – tạo hồ sơ thành công: gán user, khoa, chuyên môn và giá trị mặc định")
    void createDoctorWithDepartment_happyPath_savesDoctor() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.existsByUserId(USER_ID)).thenReturn(false);
        when(departmentRepository.findById(DEPARTMENT_ID)).thenReturn(Optional.of(department));
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(inv -> inv.getArgument(0));

        Doctor result = service.createDoctorWithDepartment(request, DEPARTMENT_ID);

        ArgumentCaptor<Doctor> captor = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorRepository).save(captor.capture());
        Doctor saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(doctorUser);
        assertThat(saved.getDepartment()).isSameAs(department);
        assertThat(saved.getSpecialization()).isEqualTo("Tim mạch");
        assertThat(saved.getQualification()).isEqualTo("Bác sĩ chuyên khoa I");
        assertThat(saved.getExperience()).isEqualTo(5);
        assertThat(saved.getAvailable()).isTrue();
        assertThat(saved.getConsultationFee()).isZero();
        assertThat(result).isSameAs(saved);
    }

    @Test
    @DisplayName("W2 – tài khoản không tồn tại → ResourceNotFoundException")
    void createDoctorWithDepartment_userNotFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createDoctorWithDepartment(request, DEPARTMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 3");

        verify(doctorRepository, never()).save(any());
    }

    @Test
    @DisplayName("W3 – tài khoản không phải DOCTOR → 400")
    void createDoctorWithDepartment_userIsNotDoctor_throws400() {
        doctorUser.setRole(User.Role.PATIENT);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(doctorUser));

        assertThatThrownBy(() -> service.createDoctorWithDepartment(request, DEPARTMENT_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(doctorRepository, never()).save(any());
    }

    @Test
    @DisplayName("W4 – tài khoản đã có hồ sơ bác sĩ → 409")
    void createDoctorWithDepartment_profileAlreadyExists_throws409() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.existsByUserId(USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.createDoctorWithDepartment(request, DEPARTMENT_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("đã có hồ sơ bác sĩ");

        verify(doctorRepository, never()).save(any());
    }

    @Test
    @DisplayName("W5 – khoa không tồn tại → ResourceNotFoundException")
    void createDoctorWithDepartment_departmentNotFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.existsByUserId(USER_ID)).thenReturn(false);
        when(departmentRepository.findById(DEPARTMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createDoctorWithDepartment(request, DEPARTMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Department not found with id: 1");

        verify(doctorRepository, never()).save(any());
    }

    @Test
    @DisplayName("W6 – xoá khoa: tìm thấy thì gọi delete")
    void deleteDepartment_existing_deletes() {
        when(departmentRepository.findById(DEPARTMENT_ID)).thenReturn(Optional.of(department));

        service.deleteDepartment(DEPARTMENT_ID);

        verify(departmentRepository).delete(department);
    }

    @Test
    @DisplayName("W7 – xoá khoa không tồn tại → không gọi delete")
    void deleteDepartment_notFound_throws() {
        when(departmentRepository.findById(DEPARTMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteDepartment(DEPARTMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(departmentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("W8 – tạo khoa thành công khi tên chưa tồn tại")
    void createDepartment_newName_saves() {
        when(departmentRepository.existsByName("Khoa Nội")).thenReturn(false);
        when(departmentRepository.save(department)).thenReturn(department);

        Department result = service.createDepartment(department);

        assertThat(result).isSameAs(department);
        verify(departmentRepository).save(department);
    }

    @Test
    @DisplayName("W9 – tạo khoa trùng tên → ném lỗi")
    void createDepartment_duplicateName_throws() {
        when(departmentRepository.existsByName("Khoa Nội")).thenReturn(true);

        assertThatThrownBy(() -> service.createDepartment(department))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");

        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("W10 – danh sách ứng viên bác sĩ được map đúng trường")
    void getDoctorCandidates_mapsFields() {
        when(userRepository.findUsersWithoutDoctorProfile(User.Role.DOCTOR))
                .thenReturn(List.of(doctorUser));

        List<DoctorCandidateResponse> result = service.getDoctorCandidates();

        assertThat(result).hasSize(1);
        DoctorCandidateResponse candidate = result.get(0);
        assertThat(candidate.id()).isEqualTo(USER_ID);
        assertThat(candidate.username()).isEqualTo("bacsi3");
        assertThat(candidate.firstName()).isEqualTo("An");
        assertThat(candidate.lastName()).isEqualTo("Nguyễn");
        assertThat(candidate.email()).isEqualTo("bacsi3@hospital.vn");
    }

    @Test
    @DisplayName("W11 – các method danh sách uỷ quyền cho repository")
    void listMethods_delegateToRepositories() {
        when(departmentRepository.findAll()).thenReturn(List.of(department));
        when(doctorRepository.findAll()).thenReturn(List.of(new Doctor()));

        assertThat(service.getAllDepartments()).containsExactly(department);
        assertThat(service.getAllDoctors()).hasSize(1);
    }
}