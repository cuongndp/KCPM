package com.hospital.management.service;

import com.hospital.management.dto.MedicalRecordRequest;
import com.hospital.management.exception.ResourceNotFoundException;
import com.hospital.management.model.Doctor;
import com.hospital.management.model.MedicalRecord;
import com.hospital.management.model.Patient;
import com.hospital.management.repository.DoctorRepository;
import com.hospital.management.repository.MedicalRecordRepository;
import com.hospital.management.repository.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private MedicalRecordService medicalRecordService;

    @Test
    @DisplayName("WB-MRC-01 — tạo hồ sơ bệnh án thành công")
    void createsMedicalRecord() {
        Doctor doctor = new Doctor();
        Patient patient = new Patient();
        MedicalRecordRequest request = new MedicalRecordRequest();
        request.setPatientId(1L);
        request.setDiagnosis("Cảm cúm");

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.save(any(MedicalRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MedicalRecord result = medicalRecordService.createMedicalRecord(1L, request);

        assertThat(result.getDiagnosis()).isEqualTo("Cảm cúm");
        assertThat(result.getDoctor()).isSameAs(doctor);
        assertThat(result.getPatient()).isSameAs(patient);
    }

    @Test
    @DisplayName("WB-MRC-02 — không tìm thấy bác sĩ")
    void throwsWhenDoctorIsMissing() {
        MedicalRecordRequest request = new MedicalRecordRequest();
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicalRecordService.createMedicalRecord(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Doctor");

        verifyNoInteractions(patientRepository, medicalRecordRepository);
    }

    @Test
    @DisplayName("WB-MRC-03 — không tìm thấy bệnh nhân")
    void throwsWhenPatientIsMissing() {
        MedicalRecordRequest request = new MedicalRecordRequest();
        request.setPatientId(99L);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(new Doctor()));
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicalRecordService.createMedicalRecord(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient");

        verifyNoInteractions(medicalRecordRepository);
        verify(doctorRepository).findById(1L);
    }
}