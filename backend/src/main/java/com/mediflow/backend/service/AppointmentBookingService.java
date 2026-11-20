package com.mediflow.backend.service;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediflow.backend.dto.AppointmentResponse;
import com.mediflow.backend.dto.BookAppointmentRequest;
import com.mediflow.backend.model.Appointment;
import com.mediflow.backend.model.AppointmentStatus;
import com.mediflow.backend.model.User;
import com.mediflow.backend.model.UserRole;
import com.mediflow.backend.repository.AppointmentRepository;
import com.mediflow.backend.repository.DoctorRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppointmentBookingService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorSlotService doctorSlotService;
    private final EntityManager entityManager;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentResponse bookAppointment(
            Long authenticatedUserId, BookAppointmentRequest request) {

        User patient = entityManager.find(User.class, authenticatedUserId);

        if (patient == null
                || patient.getRole() != UserRole.PATIENT
                || !Boolean.TRUE.equals(patient.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only active patients can book appointments");
        }

        var doctor = doctorRepository.findByIdForBooking(request.doctorId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Doctor not found"));

        // Recheck availability after acquiring the doctor lock.
        var availableSlots = doctorSlotService.getSlots(
                doctor.getId(), request.startTime().toLocalDate());

        var selectedSlot = availableSlots.slots().stream()
                .filter(slot -> slot.startTime().equals(request.startTime()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This slot is unavailable. Please select another slot."));

        var appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setStartTime(selectedSlot.startTime());
        appointment.setEndTime(selectedSlot.endTime());
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        var saved = appointmentRepository.save(appointment);
        return AppointmentResponse.from(saved);
    }

    @Transactional
    public AppointmentResponse cancelMyAppointment(
            Long patientId, Long appointmentId, String reason) {

        var appointment = appointmentRepository
                .findOwnedAppointmentForUpdate(appointmentId, patientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only confirmed appointments can be cancelled");
        }

        var now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));

        if (!appointment.getStartTime().isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Appointment has already started");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(
                reason == null || reason.isBlank() ? null : reason.strip());
        appointment.setCancelledByUser(appointment.getPatient());
        appointment.setCancelledAt(now);

        var audit = new com.mediflow.backend.model.AuditLog();
        audit.setActorUser(appointment.getPatient());
        audit.setAction("APPOINTMENT_CANCELLED");
        audit.setEntityType("Appointment");
        audit.setEntityId(appointmentId);
        entityManager.persist(audit);
        return AppointmentResponse.from(appointment);
    }
}