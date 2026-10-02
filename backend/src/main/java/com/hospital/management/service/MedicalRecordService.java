package com.hospital.management.service;

import com.hospital.management.dto.MedicalRecordRequest;
import com.hospital.management.exception.ResourceNotFoundException;
import com.hospital.management.model.Doctor;
import com.hospital.management.model.MedicalRecord;
import com.hospital.management.model.Patient;
import com.hospital.management.repository.DoctorRepository;
import com.hospital.management.repository.MedicalRecordRepository;
import com.hospital.management.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MedicalRecordService {

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    public List<MedicalRecord> getPatientMedicalHistory(Long patientId) {
        return medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public List<MedicalRecord> getDoctorMedicalRecords(Long doctorId) {
        return medicalRecordRepository.findByDoctorId(doctorId);
    }

    public MedicalRecord createMedicalRecord(Long doctorId, MedicalRecordRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", request.getPatientId()));

        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setPatient(patient);
        medicalRecord.setDoctor(doctor);
        medicalRecord.setDiagnosis(request.getDiagnosis());
        medicalRecord.setSymptoms(request.getSymptoms());
        medicalRecord.setConsultationNotes(request.getConsultationNotes());
        medicalRecord.setLabResults(request.getLabResults());
        medicalRecord.setRecommendations(request.getRecommendations());

        return medicalRecordRepository.save(medicalRecord);
    }

}