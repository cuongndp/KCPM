package com.hospital.management.dto;

public record DoctorCandidateResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        String email) {
}