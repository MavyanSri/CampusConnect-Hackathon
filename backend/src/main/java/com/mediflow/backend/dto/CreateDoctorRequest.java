package com.mediflow.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateDoctorRequest(

        @NotBlank
        @Size(min = 2, max = 100)
        String fullName,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String temporaryPassword,

        @Size(max = 20)
        @Pattern(regexp = "\\+?[0-9][0-9 ()-]{5,18}[0-9]")
        String phoneNumber,

        @NotNull
        @Positive
        Long specializationId,

        @NotBlank
        @Size(max = 255)
        String qualification,

        @NotBlank
        @Size(max = 100)
        String licenseNumber,

        @NotNull
        @Min(0)
        @Max(70)
        Integer experienceYears,

        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100000.00")
        @Digits(integer = 6, fraction = 2)
        BigDecimal consultationFee,

        @NotNull
        @Min(30)
        @Max(30)
        Integer consultationDurationMinutes,

        List<@NotBlank String> languages,

        @Size(max = 2000)
        String bio
) {
    public CreateDoctorRequest {
        if (fullName != null) {
            fullName = fullName.strip();
        }
        if (email != null) {
            email = email.strip().toLowerCase(Locale.ROOT);
        }
        phoneNumber = phoneNumber == null || phoneNumber.isBlank()
                ? null : phoneNumber.strip();
        if (qualification != null) {
            qualification = qualification.strip();
        }
        if (licenseNumber != null) {
            licenseNumber = licenseNumber.strip();
        }
    }
}