package com.mediflow.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediflow.backend.dto.DoctorSlotsResponse;
import com.mediflow.backend.dto.DoctorSlotsResponse.Slot;
import com.mediflow.backend.model.AppointmentStatus;
import com.mediflow.backend.repository.AppointmentRepository;
import com.mediflow.backend.repository.DoctorAvailabilityRepository;
import com.mediflow.backend.repository.DoctorRepository;
import com.mediflow.backend.repository.DoctorTimeOffRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DoctorSlotService {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Kolkata");

    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final DoctorTimeOffRepository timeOffRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public DoctorSlotsResponse getSlots(Long doctorId, LocalDate date) {
        if (date.isBefore(LocalDate.now(CLINIC_ZONE))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose today or a future date");
        }
        var doctor = doctorRepository.findByIdAndActiveTrueAndUser_ActiveTrue(doctorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Doctor not found"));

        int duration = doctor.getConsultationDurationMinutes();
        if (duration <= 0) {
            throw new IllegalStateException("Doctor consultation duration must be positive");
        }

        var dayStart = date.atStartOfDay();
        var dayEnd = date.plusDays(1).atStartOfDay();
        var now = LocalDateTime.now(CLINIC_ZONE);

        var workingHours = availabilityRepository
                .findByDoctor_IdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(
                        doctorId, date.getDayOfWeek());

        var timeOff = timeOffRepository
                .findByDoctor_IdAndStartTimeLessThanAndEndTimeGreaterThan(
                        doctorId, dayEnd, dayStart);

        var bookings = appointmentRepository
                .findByDoctor_IdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                        doctorId, AppointmentStatus.CONFIRMED, dayEnd, dayStart);

        var slots = new ArrayList<Slot>();

        for (var hours : workingHours) {
            var closingTime = date.atTime(hours.getEndTime());
            var start = date.atTime(hours.getStartTime());

            while (!start.plusMinutes(duration).isAfter(closingTime)) {
                var slotStart = start;
                var slotEnd = start.plusMinutes(duration);

                boolean onLeave = timeOff.stream().anyMatch(leave ->
                        leave.getStartTime().isBefore(slotEnd)
                        && leave.getEndTime().isAfter(slotStart));

                boolean booked = bookings.stream().anyMatch(appointment ->
                        appointment.getStartTime().isBefore(slotEnd)
                        && appointment.getEndTime().isAfter(slotStart));

                if (slotStart.isAfter(now) && !onLeave && !booked) {
                    slots.add(new Slot(slotStart, slotEnd));
                }

                start = slotEnd;
            }
        }

        return new DoctorSlotsResponse(
                doctorId, date, CLINIC_ZONE.getId(), duration, slots);
    }
}