package com.mediflow.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.mediflow.backend.dto.AppointmentResponse;
import com.mediflow.backend.dto.BookAppointmentRequest;
import com.mediflow.backend.model.AppointmentStatus;
import com.mediflow.backend.model.User;
import com.mediflow.backend.model.UserRole;
import com.mediflow.backend.repository.AppointmentRepository;
import com.mediflow.backend.repository.UserRepository;
import com.mediflow.backend.service.AppointmentBookingService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentBookingService bookingService;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public record CancelAppointmentRequest(
            @Size(max = 500) String cancellationReason
    ) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse bookAppointment(
            Authentication authentication,
            @Valid @RequestBody BookAppointmentRequest request) {

        User patient = getCurrentPatient(authentication);
        return bookingService.bookAppointment(patient.getId(), request);
    }

    @GetMapping("/my")
    public List<AppointmentResponse> getMyAppointments(
            Authentication authentication,
            @RequestParam(name = "status", required = false)
            AppointmentStatus status) {

        User patient = getCurrentPatient(authentication);

        return appointmentRepository.findMyAppointments(patient.getId(), status)
                .stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    @PatchMapping("/{id}/cancel")
    public AppointmentResponse cancelMyAppointment(
            Authentication authentication,
            @PathVariable("id") Long appointmentId,
            @Valid @RequestBody CancelAppointmentRequest request) {

        User patient = getCurrentPatient(authentication);

        return bookingService.cancelMyAppointment(
                patient.getId(),
                appointmentId,
                request.cancellationReason());
    }

    private User getCurrentPatient(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Login account not found"));

        if (user.getRole() != UserRole.PATIENT
                || !Boolean.TRUE.equals(user.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "An active patient account is required");
        }

        return user;
    }
}