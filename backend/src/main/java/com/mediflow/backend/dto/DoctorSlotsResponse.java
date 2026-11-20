package com.mediflow.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public record DoctorSlotsResponse(
        Long doctorId,
        LocalDate date,
        String timezone,
        int consultationDurationMinutes,
        List<Slot> slots
) {
    public record Slot(
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTime,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endTime
    ) {}
}