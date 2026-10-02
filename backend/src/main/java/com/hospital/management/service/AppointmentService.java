package com.hospital.management.service;

import com.hospital.management.dto.AppointmentRequest;
import com.hospital.management.exception.ResourceNotFoundException;
import com.hospital.management.model.Appointment;
import com.hospital.management.model.Department;
import com.hospital.management.model.Doctor;
import com.hospital.management.model.Patient;
import com.hospital.management.repository.AppointmentRepository;
import com.hospital.management.repository.DepartmentRepository;
import com.hospital.management.repository.DoctorRepository;
import com.hospital.management.repository.PatientRepository;
import com.hospital.management.util.BusinessTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    public List<Appointment> getPatientAppointments(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    public List<Appointment> getDoctorAppointments(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    public Appointment bookAppointment(Long patientId, AppointmentRequest request) {
        if (!request.getAppointmentDateTime().isAfter(BusinessTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment must be in the future");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", request.getDepartmentId()));
        if (!department.getActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Khoa đã tạm dừng nhận lịch hẹn.");
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", request.getDoctorId()));

        if (!doctor.getAvailable()) {
            throw new RuntimeException("Doctor is not available for appointments");
        }
        if (!doctor.getDepartment().getId().equals(department.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bác sĩ không thuộc khoa đã chọn.");
        }

        List<Appointment> conflictingAppointments = appointmentRepository.findConflictingAppointments(
                request.getDoctorId(), request.getAppointmentDateTime());

        if (!conflictingAppointments.isEmpty()) {
            throw new RuntimeException("Time slot is already booked");
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDateTime(request.getAppointmentDateTime());
        appointment.setStatus(Appointment.Status.SCHEDULED);
        appointment.setReason(request.getReason());
        appointment.setNotes(request.getNotes());
        appointment.setReminderSent(false);
        appointment.setFollowUpRequired(false);

        return appointmentRepository.save(appointment);
    }

    public Appointment updateAppointmentStatus(Long appointmentId, Appointment.Status status) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
    }

    public void cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        appointment.setStatus(Appointment.Status.CANCELLED);
        appointmentRepository.save(appointment);
    }

    public List<Appointment> getUpcomingAppointments(Long doctorId) {
        LocalDateTime now = BusinessTime.now();
        return appointmentRepository.findDoctorAppointmentsBetween(
            doctorId, now, now.plusDays(7));
    }
}