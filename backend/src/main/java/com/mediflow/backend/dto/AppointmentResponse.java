package com.mediflow.backend.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mediflow.backend.model.Appointment;
import com.mediflow.backend.model.AppointmentStatus;

public record AppointmentResponse(
        Long id,
        Long doctorId,
        String doctorName,
        String specializationName,
        String patientName,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTime,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endTime,
        AppointmentStatus status,
        String cancellationReason,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt
) {
    public static AppointmentResponse from(Appointment appointment) {
        var doctor = appointment.getDoctor();

        return new AppointmentResponse(
                appointment.getId(),
                doctor.getId(),
                doctor.getUser().getFullName(),
                doctor.getSpecialization().getName(),
                appointment.getPatient().getFullName(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getCancellationReason(),
                appointment.getCreatedAt()
        );
    }
}