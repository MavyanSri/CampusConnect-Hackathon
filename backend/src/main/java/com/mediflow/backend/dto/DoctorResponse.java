package com.mediflow.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import com.mediflow.backend.model.Doctor;

public record DoctorResponse(
        Long id,
        String fullName,
        Long specializationId,
        String specializationName,
        String qualification,
        String licenseNumber,
        Integer experienceYears,
        BigDecimal consultationFee,
        Integer consultationDurationMinutes,
        List<String> languages,
        String bio,
        String profileImageUrl,
        Boolean active
) {
    public static DoctorResponse from(Doctor doctor) {
        var user = doctor.getUser();
        var specialization = doctor.getSpecialization();

        return new DoctorResponse(
                doctor.getId(),
                user.getFullName(),
                specialization.getId(),
                specialization.getName(),
                doctor.getQualification(),
                doctor.getLicenseNumber(),
                doctor.getExperienceYears(),
                doctor.getConsultationFee(),
                doctor.getConsultationDurationMinutes(),
                doctor.getLanguages() == null ? List.of() : doctor.getLanguages(),
                doctor.getBio(),
                user.getProfileImageUrl(),
                doctor.getActive()
        );
    }
}