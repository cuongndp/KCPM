package com.hospital.management.service;

import com.hospital.management.exception.ResourceNotFoundException;
import com.hospital.management.model.Appointment;
import com.hospital.management.repository.AppointmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test cho API 1: PUT /api/doctor/appointments/{appointmentId}/status
 * Method được kiểm thử: AppointmentService.updateAppointmentStatus (dòng 91-96).
 *
 * WB-DAS-01  P1  n1-d2-n4-n5-END
 * WB-DAS-02  P2  n1-d2-n3-END
 */
@ExtendWith(MockitoExtension.class)
class UpdateAppointmentStatusTest {

    private static final Long APPOINTMENT_ID = 1L;
    private static final Long MISSING_ID = 99L;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    @DisplayName("WB-DAS-01: cập nhật trạng thái và lưu khi tìm thấy lịch hẹn")
    void updatesStatus_whenAppointmentFound() {
        Appointment existing = new Appointment();
        existing.setStatus(Appointment.Status.SCHEDULED);

        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(existing));
        when(appointmentRepository.save(existing)).thenReturn(existing);

        Appointment result = appointmentService.updateAppointmentStatus(
                APPOINTMENT_ID, Appointment.Status.CONFIRMED);

        assertNotNull(result);
        assertEquals(Appointment.Status.CONFIRMED, result.getStatus());

        ArgumentCaptor<Appointment> saved = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository, times(1)).save(saved.capture());
        assertEquals(Appointment.Status.CONFIRMED, saved.getValue().getStatus());
        verify(appointmentRepository, times(1)).findById(APPOINTMENT_ID);
    }

    @Test
    @DisplayName("WB-DAS-02: ném ResourceNotFoundException và không lưu khi không tìm thấy lịch hẹn")
    void throws_whenAppointmentMissing() {
        when(appointmentRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> appointmentService.updateAppointmentStatus(MISSING_ID, Appointment.Status.CONFIRMED));

        assertTrue(ex.getMessage().contains("Appointment"),
                "Thông điệp phải nêu tên tài nguyên Appointment");
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }
}
