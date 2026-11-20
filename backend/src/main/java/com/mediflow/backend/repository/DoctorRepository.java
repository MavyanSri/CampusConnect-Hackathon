package com.mediflow.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mediflow.backend.model.Doctor;

import jakarta.persistence.LockModeType;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    @EntityGraph(attributePaths = {"user", "specialization"})
    @Query("""
            select d
            from Doctor d
            where d.active = true
              and d.user.active = true
              and (:search = ''
                   or locate(lower(:search), lower(d.user.fullName)) > 0)
              and (:specializationId is null
                   or d.specialization.id = :specializationId)
            order by d.user.fullName asc, d.id asc
            """)
    Page<Doctor> searchActiveDoctors(
            @Param("search") String search,
            @Param("specializationId") Long specializationId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "specialization"})
    Optional<Doctor> findByIdAndActiveTrueAndUser_ActiveTrue(Long id);

    @EntityGraph(attributePaths = {"user", "specialization"})
    Optional<Doctor> findByUser_Email(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Doctor d where d.id = :doctorId")
    Optional<Doctor> findByIdForBooking(@Param("doctorId") Long doctorId);
}