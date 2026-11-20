package com.mediflow.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mediflow.backend.dto.CreateDoctorRequest;
import com.mediflow.backend.service.AdminDoctorService;
import com.mediflow.backend.service.AdminDoctorService.AdminDoctorResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/doctors")
@RequiredArgsConstructor
public class AdminDoctorController {

    private final AdminDoctorService adminDoctorService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminDoctorResponse createDoctor(
            Authentication authentication,
            @Valid @RequestBody CreateDoctorRequest request) {

        return adminDoctorService.createDoctor(authentication.getName(), request);
    }
}