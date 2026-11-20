package com.mediflow.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediflow.backend.model.DoctorTimeOff;

public interface DoctorTimeOffRepository extends JpaRepository<DoctorTimeOff, Long> {

    List<DoctorTimeOff> findByDoctor_IdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long doctorId, LocalDateTime dayEnd, LocalDateTime dayStart);
}