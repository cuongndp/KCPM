package com.hospital.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorAvailabilityRequest {
    @NotNull(message = "Trạng thái làm việc là bắt buộc")
    private Boolean available;
}