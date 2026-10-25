package com.hospital.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecordRequest {
    @NotNull(message = "Patient ID is required")
    private Long patientId;
    
    @NotBlank(message = "Diagnosis is required")
    private String diagnosis;
    
    private String symptoms;
    private String consultationNotes;
    private String labResults;
    private String recommendations;
}