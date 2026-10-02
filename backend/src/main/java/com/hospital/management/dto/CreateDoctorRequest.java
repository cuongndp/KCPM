package com.hospital.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDoctorRequest {
    @NotNull(message = "Vui lòng chọn tài khoản bác sĩ")
    private Long userId;

    @NotBlank(message = "Chuyên môn là bắt buộc")
    private String specialization;

    private String qualification;

    private Integer experience;
}