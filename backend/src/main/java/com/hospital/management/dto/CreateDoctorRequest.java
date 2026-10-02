package com.hospital.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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

    @PositiveOrZero(message = "Kinh nghiệm không được âm")
    private Integer experience;

    @NotNull(message = "Phí khám là bắt buộc")
    @PositiveOrZero(message = "Phí khám không được âm")
    private Double consultationFee;
}