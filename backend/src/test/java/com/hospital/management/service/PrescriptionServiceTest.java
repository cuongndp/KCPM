package com.hospital.management.service;

import com.hospital.management.dto.PrescriptionRequest;
import com.hospital.management.exception.ResourceNotFoundException;
import com.hospital.management.model.Appointment;
import com.hospital.management.model.Doctor;
import com.hospital.management.model.Patient;
import com.hospital.management.model.Prescription;
import com.hospital.management.repository.AppointmentRepository;
import com.hospital.management.repository.DoctorRepository;
import com.hospital.management.repository.PatientRepository;
import com.hospital.management.repository.PrescriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private PrescriptionService prescriptionService;

    @Test
    @DisplayName("WB-PRS-01 â€” táº¡o Ä‘Æ¡n thuá»‘c khÃ´ng gáº¯n lá»‹ch háº¹n")
    void createsPrescriptionWithoutAppointment() {
        Doctor doctor = new Doctor();
        Patient patient = new Patient();
        PrescriptionRequest request = request(1L, null);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(prescriptionRepository.save(any(Prescription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Prescription result = prescriptionService.createPrescription(1L, request);

        assertThat(result.getAppointment()).isNull();
        assertThat(result.getNotificationSent()).isEqualTo(Boolean.FALSE);
        assertThat(result.getMedication()).isEqualTo("Paracetamol");
    }

    @Test
    @DisplayName("WB-PRS-02 â€” gáº¯n lá»‹ch háº¹n tá»“n táº¡i")
    void createsPrescriptionWithExistingAppointment() {
        Doctor doctor = new Doctor();
        Patient patient = new Patient();
        Appointment appointment = new Appointment();
        appointment.setId(2L);
        PrescriptionRequest request = request(1L, 2L);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(appointmentRepository.findById(2L)).thenReturn(Optional.of(appointment));
        when(prescriptionRepository.save(any(Prescription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Prescription result = prescriptionService.createPrescription(1L, request);

        assertThat(result.getAppointment()).isSameAs(appointment);
    }

    @Test
    @DisplayName("WB-PRS-03 â€” khÃ´ng tÃ¬m tháº¥y bÃ¡c sÄ©")
    void throwsWhenDoctorIsMissing() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prescriptionService.createPrescription(99L, request(1L, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Doctor");

        verifyNoInteractions(patientRepository, appointmentRepository, prescriptionRepository);
    }

    @Test
    @DisplayName("WB-PRS-04 â€” khÃ´ng tÃ¬m tháº¥y bá»‡nh nhÃ¢n")
    void throwsWhenPatientIsMissing() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(new Doctor()));
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prescriptionService.createPrescription(1L, request(99L, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient");

        verifyNoInteractions(appointmentRepository, prescriptionRepository);
    }

    @Test
    @DisplayName("WB-PRS-05 â€” khÃ´ng tÃ¬m tháº¥y lá»‹ch háº¹n")
    void throwsWhenAppointmentIsMissing() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(new Doctor()));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(new Patient()));
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prescriptionService.createPrescription(1L, request(1L, 99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Appointment");

        verify(prescriptionRepository, never()).save(any(Prescription.class));
    }

    private PrescriptionRequest request(Long patientId, Long appointmentId) {
        PrescriptionRequest request = new PrescriptionRequest();
        request.setPatientId(patientId);
        request.setAppointmentId(appointmentId);
        request.setMedication("Paracetamol");
        request.setDosage("500 mg");
        request.setFrequency("Twice daily");
        return request;
    }
}