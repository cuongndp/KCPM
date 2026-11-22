package com.hospital.management.service;

import com.hospital.management.exception.ResourceNotFoundException;
import com.hospital.management.model.Department;
import com.hospital.management.model.Doctor;
import com.hospital.management.repository.AppointmentRepository;
import com.hospital.management.repository.DepartmentRepository;
import com.hospital.management.repository.DoctorRepository;
import com.hospital.management.repository.MedicalRecordRepository;
import com.hospital.management.repository.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class DoctorServiceTest {

    private static final Long DOCTOR_ID = 1L;
    private static final Long USER_ID = 3L;
    private static final Long DEPARTMENT_ID = 1L;

    @Mock private DoctorRepository doctorRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private PrescriptionRepository prescriptionRepository;

    @InjectMocks private DoctorService service;

    private Doctor doctor;

    @BeforeEach
    void setUp() {
        doctor = new Doctor();
        doctor.setId(DOCTOR_ID);
        doctor.setAvailable(true);
    }

    @Test
    @DisplayName("W1 – danh sách bác sĩ đang sẵn sàng")
    void getAvailableDoctors_returnsAvailable() {
        when(doctorRepository.findByAvailableTrue()).thenReturn(List.of(doctor));

        assertThat(service.getAvailableDoctors()).containsExactly(doctor);
    }

    @Test
    @DisplayName("W2 – danh sách khoa đang hoạt động")
    void getActiveDepartments_returnsActive() {
        Department department = new Department();
        department.setId(DEPARTMENT_ID);
        when(departmentRepository.findByActiveTrue()).thenReturn(List.of(department));

        assertThat(service.getActiveDepartments()).containsExactly(department);
    }

    @Test
    @DisplayName("W3 – bác sĩ sẵn sàng theo khoa")
    void getAvailableDoctorsByDepartment_filters() {
        when(doctorRepository.findByDepartmentIdAndDepartmentActiveTrueAndAvailableTrue(DEPARTMENT_ID))
                .thenReturn(List.of(doctor));

        assertThat(service.getAvailableDoctors(DEPARTMENT_ID)).containsExactly(doctor);
    }

    @Test
    @DisplayName("W4 – tìm bác sĩ theo id: thấy thì trả về")
    void getDoctorById_found_returnsDoctor() {
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));

        assertThat(service.getDoctorById(DOCTOR_ID)).isSameAs(doctor);
    }

    @Test
    @DisplayName("W5 – tìm bác sĩ theo id: không thấy → ResourceNotFoundException")
    void getDoctorById_notFound_throws() {
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDoctorById(DOCTOR_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Doctor not found with id: 1");
    }

    @Test
    @DisplayName("W6 – tìm bác sĩ theo userId: thấy thì trả về")
    void getDoctorByUserId_found_returnsDoctor() {
        when(doctorRepository.findByUserId(USER_ID)).thenReturn(Optional.of(doctor));

        assertThat(service.getDoctorByUserId(USER_ID)).isSameAs(doctor);
    }

    @Test
    @DisplayName("W7 – tài khoản chưa được cấp hồ sơ → 403")
    void getDoctorByUserId_notFound_throws403() {
        when(doctorRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDoctorByUserId(USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN))
                .hasMessageContaining("chưa được quản trị viên cấp hồ sơ");
    }

    @Test
    @DisplayName("W8 – xoá bác sĩ không vướng dữ liệu liên quan")
    void deleteDoctor_noDependencies_deletes() {
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByDoctorId(DOCTOR_ID)).thenReturn(false);
        when(medicalRecordRepository.existsByDoctorId(DOCTOR_ID)).thenReturn(false);
        when(prescriptionRepository.existsByDoctorId(DOCTOR_ID)).thenReturn(false);

        service.deleteDoctor(DOCTOR_ID);

        verify(doctorRepository).delete(doctor);
    }

    @Test
    @DisplayName("W9 – còn cuộc hẹn → 409, không xoá")
    void deleteDoctor_hasAppointment_throws409() {
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByDoctorId(DOCTOR_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteDoctor(DOCTOR_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("Không thể xóa bác sĩ");

        verify(medicalRecordRepository, never()).existsByDoctorId(any());
        verify(doctorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("W10 – chỉ còn hồ sơ y tế → 409")
    void deleteDoctor_hasMedicalRecord_throws409() {
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByDoctorId(DOCTOR_ID)).thenReturn(false);
        when(medicalRecordRepository.existsByDoctorId(DOCTOR_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteDoctor(DOCTOR_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(prescriptionRepository, never()).existsByDoctorId(any());
        verify(doctorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("W11 – chỉ còn đơn thuốc → 409")
    void deleteDoctor_hasPrescription_throws409() {
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByDoctorId(DOCTOR_ID)).thenReturn(false);
        when(medicalRecordRepository.existsByDoctorId(DOCTOR_ID)).thenReturn(false);
        when(prescriptionRepository.existsByDoctorId(DOCTOR_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteDoctor(DOCTOR_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(doctorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("W12 – xoá bác sĩ không tồn tại → ResourceNotFoundException")
    void deleteDoctor_notFound_throws() {
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteDoctor(DOCTOR_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(doctorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("W13 – bật sẵn sàng")
    void updateAvailability_true_saves() {
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(doctorRepository.save(doctor)).thenReturn(doctor);

        Doctor result = service.updateAvailability(DOCTOR_ID, true);

        assertThat(result.getAvailable()).isTrue();
        verify(doctorRepository).save(doctor);
    }

    @Test
    @DisplayName("W14 – tắt sẵn sàng")
    void updateAvailability_false_saves() {
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(doctorRepository.save(doctor)).thenReturn(doctor);

        Doctor result = service.updateAvailability(DOCTOR_ID, false);

        assertThat(result.getAvailable()).isFalse();
        verify(doctorRepository).save(doctor);
    }
}