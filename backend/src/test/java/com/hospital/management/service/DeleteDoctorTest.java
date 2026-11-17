package com.hospital.management.service;

import com.hospital.management.exception.ResourceNotFoundException;
import com.hospital.management.model.Doctor;
import com.hospital.management.repository.AppointmentRepository;
import com.hospital.management.repository.DoctorRepository;
import com.hospital.management.repository.MedicalRecordRepository;
import com.hospital.management.repository.PrescriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test cho API 3: DELETE /api/admin/doctors/{doctorId}
 * Method được kiểm thử: DoctorService.deleteDoctor (dòng 63-73).
 *
 * WB-ADR-01  P1  n1-n2-d3-n5-END   xoá thành công
 * WB-ADR-02  P2  n1-n2-d3-n4-END   còn lịch hẹn
 * WB-ADR-03  P2  n1-n2-d3-n4-END   còn hồ sơ y tế
 * WB-ADR-04  P2  n1-n2-d3-n4-END   còn đơn thuốc
 * WB-ADR-05  P3  n1-n2-END         không tìm thấy bác sĩ
 *
 * Ba toán hạng || được đánh giá short-circuit: mỗi test chỉ stub tới điều kiện
 * cần thiết để tránh UnnecessaryStubbingException của MockitoExtension.
 */
@ExtendWith(MockitoExtension.class)
class DeleteDoctorTest {

    private static final Long DOCTOR_ID = 1L;
    private static final Long MISSING_ID = 99L;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @InjectMocks
    private DoctorService doctorService;

    @Test
    @DisplayName("WB-ADR-01: xoá hồ sơ bác sĩ khi không còn dữ liệu liên quan")
    void removesDoctor_whenNoRelatedData() {
        Doctor existing = new Doctor();

        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(existing));
        when(appointmentRepository.existsByDoctorId(DOCTOR_ID)).thenReturn(false);
        when(medicalRecordRepository.existsByDoctorId(DOCTOR_ID)).thenReturn(false);
        when(prescriptionRepository.existsByDoctorId(DOCTOR_ID)).thenReturn(false);

        doctorService.deleteDoctor(DOCTOR_ID);

        verify(doctorRepository, times(1)).delete(existing);
        verify(doctorRepository, times(1)).findById(DOCTOR_ID);
    }

    @Test
    @DisplayName("WB-ADR-02: trả 409 và không xoá khi bác sĩ còn lịch hẹn")
    void throws409_whenAppointmentExists() {
        Doctor existing = new Doctor();

        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(existing));
        when(appointmentRepository.existsByDoctorId(DOCTOR_ID)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> doctorService.deleteDoctor(DOCTOR_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason() != null && ex.getReason().contains("Không thể xóa bác sĩ"),
                "Thông điệp phải nêu lý do không thể xoá bác sĩ");
        verify(doctorRepository, never()).delete(any(Doctor.class));
    }

    @Test
    @DisplayName("WB-ADR-03: trả 409 và không xoá khi bác sĩ còn hồ sơ y tế")
    void throws409_whenMedicalRecordExists() {
        Doctor existing = new Doctor();

        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(existing));
        when(appointmentRepository.existsByDoctorId(DOCTOR_ID)).thenReturn(false);
        when(medicalRecordRepository.existsByDoctorId(DOCTOR_ID)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> doctorService.deleteDoctor(DOCTOR_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason() != null && ex.getReason().contains("hồ sơ y tế"),
                "Thông điệp phải nêu hồ sơ y tế");
        verify(doctorRepository, never()).delete(any(Doctor.class));
        verify(prescriptionRepository, never()).existsByDoctorId(any());
    }

    @Test
    @DisplayName("WB-ADR-04: trả 409 và không xoá khi bác sĩ còn đơn thuốc")
    void throws409_whenPrescriptionExists() {
        Doctor existing = new Doctor();

        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(existing));
        when(appointmentRepository.existsByDoctorId(DOCTOR_ID)).thenReturn(false);
        when(medicalRecordRepository.existsByDoctorId(DOCTOR_ID)).thenReturn(false);
        when(prescriptionRepository.existsByDoctorId(DOCTOR_ID)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> doctorService.deleteDoctor(DOCTOR_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason() != null && ex.getReason().contains("đơn thuốc"),
                "Thông điệp phải nêu đơn thuốc");
        verify(doctorRepository, never()).delete(any(Doctor.class));
    }

    @Test
    @DisplayName("WB-ADR-05: ném ResourceNotFoundException và không xoá khi không tìm thấy bác sĩ")
    void throws_whenDoctorMissing() {
        when(doctorRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> doctorService.deleteDoctor(MISSING_ID));

        assertTrue(ex.getMessage().contains("Doctor"),
                "Thông điệp phải nêu tên tài nguyên Doctor");
        verify(doctorRepository, never()).delete(any(Doctor.class));
        verify(appointmentRepository, never()).existsByDoctorId(any());
    }
}
