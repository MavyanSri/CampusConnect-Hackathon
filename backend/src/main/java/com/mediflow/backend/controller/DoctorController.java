package com.mediflow.backend.controller;

import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.mediflow.backend.dto.DoctorResponse;
import com.mediflow.backend.dto.DoctorSlotsResponse;
import com.mediflow.backend.dto.PageResponse;
import com.mediflow.backend.repository.DoctorRepository;
import com.mediflow.backend.service.DoctorSlotService;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final DoctorSlotService doctorSlotService;

    public DoctorController(DoctorRepository doctorRepository,
                            DoctorSlotService doctorSlotService) {
        this.doctorRepository = doctorRepository;
        this.doctorSlotService = doctorSlotService;
    }

    @GetMapping
    public PageResponse<DoctorResponse> getDoctors(
            @RequestParam(defaultValue = "") @Size(max = 100) String search,
            @RequestParam(required = false) Long specializationId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {

        var doctors = doctorRepository.searchActiveDoctors(
                search.trim(),
                specializationId,
                PageRequest.of(page, size)
        );

        return PageResponse.from(doctors.map(DoctorResponse::from));
    }

    @GetMapping("/{id}")
    public DoctorResponse getDoctorById(@PathVariable("id") Long id) {
        return doctorRepository.findByIdAndActiveTrueAndUser_ActiveTrue(id)
                .map(DoctorResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Doctor not found"));
    }

    @GetMapping("/{id}/slots")
    public DoctorSlotsResponse getSlots(
            @PathVariable("id") Long id,
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return doctorSlotService.getSlots(id, date);
    }
}