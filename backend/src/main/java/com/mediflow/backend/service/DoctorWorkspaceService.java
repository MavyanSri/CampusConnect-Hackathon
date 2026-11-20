package com.mediflow.backend.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mediflow.backend.model.Appointment;
import com.mediflow.backend.model.AppointmentStatus;
import com.mediflow.backend.model.Doctor;
import com.mediflow.backend.model.DoctorAvailability;
import com.mediflow.backend.model.UserRole;
import com.mediflow.backend.repository.AppointmentRepository;
import com.mediflow.backend.repository.DoctorAvailabilityRepository;
import com.mediflow.backend.repository.DoctorRepository;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DoctorWorkspaceService {

    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;

    public record AvailabilityRequest(
            @NotNull DayOfWeek dayOfWeek,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            @NotNull Boolean active
    ) {}

    public record AvailabilityResponse(
            Long id,
            DayOfWeek dayOfWeek,
            @JsonFormat(pattern = "HH:mm:ss") LocalTime startTime,
            @JsonFormat(pattern = "HH:mm:ss") LocalTime endTime,
            Boolean active
    ) {
        static AvailabilityResponse from(DoctorAvailability hours) {
            return new AvailabilityResponse(
                    hours.getId(),
                    hours.getDayOfWeek(),
                    hours.getStartTime(),
                    hours.getEndTime(),
                    hours.getActive()
            );
        }
    }

    public record DoctorAppointmentResponse(
            Long id,
            String patientName,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTime,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endTime,
            AppointmentStatus status,
            String cancellationReason
    ) {
        static DoctorAppointmentResponse from(Appointment appointment) {
            return new DoctorAppointmentResponse(
                    appointment.getId(),
                    appointment.getPatient().getFullName(),
                    appointment.getStartTime(),
                    appointment.getEndTime(),
                    appointment.getStatus(),
                    appointment.getCancellationReason()
            );
        }
    }

    private Doctor currentDoctor(String email) {
        Doctor doctor = doctorRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Doctor account required"));

        if (doctor.getUser().getRole() != UserRole.DOCTOR
                || !Boolean.TRUE.equals(doctor.getUser().getActive())
                || !Boolean.TRUE.equals(doctor.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Active doctor account required");
        }

        return doctor;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getAvailability(String email) {
        Doctor doctor = currentDoctor(email);

        return availabilityRepository
                .findByDoctor_IdOrderByDayOfWeekAscStartTimeAsc(doctor.getId())
                .stream()
                .map(AvailabilityResponse::from)
                .toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AvailabilityResponse addAvailability(
            String email, AvailabilityRequest request) {

        Doctor doctor = currentDoctor(email);

        if (!request.startTime().isBefore(request.endTime())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Start time must be before end time");
        }

        // Schedule changes and bookings use the same doctor lock.
        doctorRepository.findByIdForBooking(doctor.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Doctor not found"));

        if (request.active()) {
            var existingHours = availabilityRepository
                    .findByDoctor_IdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(
                            doctor.getId(), request.dayOfWeek());

            boolean overlaps = existingHours.stream().anyMatch(hours ->
                    hours.getStartTime().isBefore(request.endTime())
                    && hours.getEndTime().isAfter(request.startTime()));

            if (overlaps) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Working hours overlap an existing schedule");
            }
        }

        var hours = new DoctorAvailability();
        hours.setDoctor(doctor);
        hours.setDayOfWeek(request.dayOfWeek());
        hours.setStartTime(request.startTime());
        hours.setEndTime(request.endTime());
        hours.setActive(request.active());

        return AvailabilityResponse.from(availabilityRepository.save(hours));
    }

    @Transactional(readOnly = true)
    public List<DoctorAppointmentResponse> getAppointments(
            String email, LocalDate date, AppointmentStatus status) {

        Doctor doctor = currentDoctor(email);
        LocalDateTime from = date == null ? null : date.atStartOfDay();
        LocalDateTime until = date == null ? null : date.plusDays(1).atStartOfDay();

        return appointmentRepository
                .findDoctorAppointments(doctor.getId(), from, until, status)
                .stream()
                .map(DoctorAppointmentResponse::from)
                .toList();
    }
}