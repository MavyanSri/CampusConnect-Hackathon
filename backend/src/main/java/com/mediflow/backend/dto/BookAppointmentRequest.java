package com.mediflow.backend.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BookAppointmentRequest(

        @NotNull
        @Positive
        Long doctorId,

        @NotNull
        LocalDateTime startTime

) {}