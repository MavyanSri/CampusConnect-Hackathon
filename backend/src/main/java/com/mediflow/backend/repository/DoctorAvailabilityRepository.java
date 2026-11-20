package com.mediflow.backend.repository;

import java.time.DayOfWeek;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediflow.backend.model.DoctorAvailability;

public interface DoctorAvailabilityRepository
        extends JpaRepository<DoctorAvailability, Long> {

    List<DoctorAvailability> findByDoctor_IdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(
            Long doctorId, DayOfWeek dayOfWeek);

    List<DoctorAvailability> findByDoctor_IdOrderByDayOfWeekAscStartTimeAsc(
            Long doctorId);
}