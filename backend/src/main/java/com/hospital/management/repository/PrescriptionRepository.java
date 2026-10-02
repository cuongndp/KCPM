package com.hospital.management.repository;

import com.hospital.management.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    boolean existsByDoctorId(Long doctorId);

    List<Prescription> findByPatientId(Long patientId);
    List<Prescription> findByDoctorId(Long doctorId);
    List<Prescription> findByAppointmentId(Long appointmentId);
    List<Prescription> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}