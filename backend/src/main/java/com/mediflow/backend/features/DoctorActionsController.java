package com.mediflow.backend.features;

import java.time.*;
import com.mediflow.backend.service.DoctorWorkspaceService.AvailabilityResponse;
import com.mediflow.backend.service.DoctorWorkspaceService.DoctorAppointmentResponse;
import java.util.List;
import java.security.Principal;
import com.mediflow.backend.model.*;
import com.mediflow.backend.repository.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.*;
import org.springframework.web.bind.annotation.*;
import static com.mediflow.backend.features.ModuleAccess.*;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class DoctorActionsController {
    private final ModuleAccess access;
    private final EntityManager em;
    private final AppointmentRepository bookings;

    public record StatusChange(@NotNull AppointmentStatus status, @Size(max=500) String cancellationReason) {}
    public record HoursChange(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, Boolean active) {}
    public record LeaveRequest(@NotNull LocalDateTime startTime, @NotNull LocalDateTime endTime,
                               @Size(max=255) String reason) {}
    public record LeaveView(Long id, @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTime,
                            @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endTime, String reason) {
        static LeaveView from(DoctorTimeOff t) {
            return new LeaveView(t.getId(), t.getStartTime(), t.getEndTime(), t.getReason());
        }
    }

    @PatchMapping("/appointments/{id}/status")
    public DoctorAppointmentResponse status(Principal principal, @PathVariable long id,
                                             @Valid @RequestBody StatusChange change) {
        Doctor doctor = access.doctor(principal.getName(), true);
        Appointment a = access.owned(Appointment.class, id, doctor);
        if (a.getStatus() != AppointmentStatus.CONFIRMED) throw conflict("Appointment has already ended or been cancelled");
        var now = LocalDateTime.now(ZONE);
        switch (change.status()) {
            case CANCELLED -> {
                if (!now.isBefore(a.getStartTime())) throw conflict("Appointment has already started");
                a.setCancelledAt(now);
                a.setCancelledByUser(doctor.getUser());
                a.setCancellationReason(change.cancellationReason());
            }
            case COMPLETED -> {
                if (now.isBefore(a.getStartTime())) throw conflict("Appointment has not started yet");
                a.setCompletedAt(now);
            }
            default -> throw invalid("Choose COMPLETED or CANCELLED");
        }
        a.setStatus(change.status());
        access.audit(doctor.getUser(), "APPOINTMENT_" + change.status(), "Appointment", id);
        return new DoctorAppointmentResponse(id, a.getPatient().getFullName(), a.getStartTime(),
                a.getEndTime(), a.getStatus(), a.getCancellationReason());
    }

    @PatchMapping("/availability/{id}")
    public AvailabilityResponse updateHours(Principal principal, @PathVariable long id, @RequestBody HoursChange change) {
        Doctor doctor = access.doctor(principal.getName(), true);
        var hours = access.owned(DoctorAvailability.class, id, doctor);
        var day = change.dayOfWeek() == null ? hours.getDayOfWeek() : change.dayOfWeek();
        var start = change.startTime() == null ? hours.getStartTime() : change.startTime();
        var end = change.endTime() == null ? hours.getEndTime() : change.endTime();
        boolean active = change.active() == null ? hours.getActive() : change.active();
        if (!start.isBefore(end)) throw invalid("Start time must be earlier than end time");
        if (active && em.createQuery("""
                select count(h) from DoctorAvailability h where h.doctor.id=:doctor
                and h.id<>:id and h.active=true and h.dayOfWeek=:day
                and h.startTime<:end and h.endTime>:start
                """, Long.class).setParameter("doctor", doctor.getId()).setParameter("id", id)
                .setParameter("day",day).setParameter("end",end).setParameter("start",start)
                .getSingleResult() > 0) {
            throw conflict("Working hours overlap");
        }
        protectBookings(doctor, hours, day, start, end, active);
        hours.setDayOfWeek(day);
        hours.setStartTime(start);
        hours.setEndTime(end);
        hours.setActive(active);
        return new AvailabilityResponse(id, day, start, end, active);
    }

    @DeleteMapping("/availability/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHours(Principal principal, @PathVariable long id) {
        Doctor doctor = access.doctor(principal.getName(), true);
        var hours = access.owned(DoctorAvailability.class, id, doctor);
        protectBookings(doctor, hours, hours.getDayOfWeek(), hours.getStartTime(), hours.getEndTime(), false);
        em.remove(hours);
    }

    private void protectBookings(Doctor doctor, DoctorAvailability old, DayOfWeek day, LocalTime start,
                                 LocalTime end, boolean active) {
        if (!old.getActive()) return;
        var confirmed = em.createQuery("select a from Appointment a where a.doctor.id = :id and a.status = :status and a.endTime > :now", Appointment.class)
                .setParameter("id", doctor.getId()).setParameter("status", AppointmentStatus.CONFIRMED)
                .setParameter("now", LocalDateTime.now(ZONE)).getResultList();
        for (var a : confirmed) {
            var s = a.getStartTime().toLocalTime();
            var e = a.getEndTime().toLocalTime();
            boolean affected = a.getStartTime().getDayOfWeek() == old.getDayOfWeek()
                    && s.isBefore(old.getEndTime()) && e.isAfter(old.getStartTime());
            if (affected && (!active || day != old.getDayOfWeek() || s.isBefore(start) || e.isAfter(end))) {
                throw conflict("An existing confirmed appointment needs these working hours");
            }
        }
    }

    @GetMapping("/time-off")
    public List<LeaveView> leave(Principal principal) {
        Doctor doctor = access.doctor(principal.getName(), false);
        return em.createQuery("select t from DoctorTimeOff t where t.doctor.id = :id order by t.startTime, t.id", DoctorTimeOff.class)
                .setParameter("id", doctor.getId()).getResultList().stream().map(LeaveView::from).toList();
    }

    @PostMapping("/time-off")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveView addLeave(Principal principal, @Valid @RequestBody LeaveRequest r) {
        Doctor doctor = access.doctor(principal.getName(), true);
        if (!r.startTime().isBefore(r.endTime())) throw invalid("Time off must end after it starts");
        if (!bookings.findByDoctor_IdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                doctor.getId(), AppointmentStatus.CONFIRMED, r.endTime(), r.startTime()).isEmpty()) {
            throw conflict("Time off overlaps a confirmed appointment");
        }
        var t = new DoctorTimeOff();
        t.setDoctor(doctor);
        t.setStartTime(r.startTime());
        t.setEndTime(r.endTime());
        t.setReason(r.reason());
        em.persist(t);
        return LeaveView.from(t);
    }

    @DeleteMapping("/time-off/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLeave(Principal principal, @PathVariable long id) {
        Doctor doctor = access.doctor(principal.getName(), true);
        em.remove(access.owned(DoctorTimeOff.class, id, doctor));
    }
}
