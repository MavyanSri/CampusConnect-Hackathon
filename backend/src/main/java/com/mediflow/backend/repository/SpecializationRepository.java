package com.mediflow.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediflow.backend.model.Specialization;

public interface SpecializationRepository
        extends JpaRepository<Specialization, Long> {

    List<Specialization> findByActiveTrueOrderByNameAsc();
}