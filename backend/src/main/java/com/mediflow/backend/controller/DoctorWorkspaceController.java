package com.mediflow.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mediflow.backend.model.AppointmentStatus;
import com.mediflow.backend.service.DoctorWorkspaceService;
import com.mediflow.backend.service.DoctorWorkspaceService.AvailabilityRequest;
import com.mediflow.backend.service.DoctorWorkspaceService.AvailabilityResponse;
import com.mediflow.backend.service.DoctorWorkspaceService.DoctorAppointmentResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
public class DoctorWorkspaceController {

    private final DoctorWorkspaceService workspaceService;

    @GetMapping("/availability")
    public List<AvailabilityResponse> getAvailability(Authentication authentication) {
        return workspaceService.getAvailability(authentication.getName());
    }

    @PostMapping("/availability")
    @ResponseStatus(HttpStatus.CREATED)
    public AvailabilityResponse addAvailability(
            Authentication authentication,
            @Valid @RequestBody AvailabilityRequest request) {

        return workspaceService.addAvailability(authentication.getName(), request);
    }

    @GetMapping("/appointments")
    public List<DoctorAppointmentResponse> getAppointments(
            Authentication authentication,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "status", required = false)
            AppointmentStatus status) {

        return workspaceService.getAppointments(
                authentication.getName(), date, status);
    }
}