package com.mediflow.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediflow.backend.dto.SpecializationResponse;
import com.mediflow.backend.repository.SpecializationRepository;

@RestController
@RequestMapping("/api/specializations")
public class SpecializationController {

    private final SpecializationRepository specializationRepository;

    public SpecializationController(
            SpecializationRepository specializationRepository) {
        this.specializationRepository = specializationRepository;
    }

    @GetMapping
    public List<SpecializationResponse> getActiveSpecializations() {
        return specializationRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(item -> new SpecializationResponse(
                        item.getId(),
                        item.getName(),
                        item.getDescription(),
                        item.getActive()))
                .toList();
    }
}