package com.mediflow.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mediflow.backend.model.Appointment;
import com.mediflow.backend.model.AppointmentStatus;

import jakarta.persistence.LockModeType;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctor_IdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            Long doctorId,
            AppointmentStatus status,
            LocalDateTime dayEnd,
            LocalDateTime dayStart
    );

    @EntityGraph(attributePaths = {
            "patient", "doctor", "doctor.user", "doctor.specialization"
    })
    @Query("""
            select a
            from Appointment a
            where a.patient.id = :patientId
              and (:status is null or a.status = :status)
            order by a.startTime desc, a.id desc
            """)
    List<Appointment> findMyAppointments(
            @Param("patientId") Long patientId,
            @Param("status") AppointmentStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a
            from Appointment a
            where a.id = :appointmentId
              and a.patient.id = :patientId
            """)
    Optional<Appointment> findOwnedAppointmentForUpdate(
            @Param("appointmentId") Long appointmentId,
            @Param("patientId") Long patientId
    );

    @EntityGraph(attributePaths = "patient")
    @Query("""
            select a
            from Appointment a
            where a.doctor.id = :doctorId
              and (:fromTime is null or a.startTime >= :fromTime)
              and (:untilTime is null or a.startTime < :untilTime)
              and (:status is null or a.status = :status)
            order by a.startTime asc, a.id asc
            """)
    List<Appointment> findDoctorAppointments(
            @Param("doctorId") Long doctorId,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("untilTime") LocalDateTime untilTime,
            @Param("status") AppointmentStatus status
    );
}