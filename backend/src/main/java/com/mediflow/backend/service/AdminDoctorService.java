package com.mediflow.backend.service;

import java.sql.SQLException;
import java.util.ArrayList;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.mediflow.backend.dto.CreateDoctorRequest;
import com.mediflow.backend.dto.DoctorResponse;
import com.mediflow.backend.model.AuditLog;
import com.mediflow.backend.model.Doctor;
import com.mediflow.backend.model.User;
import com.mediflow.backend.model.UserRole;
import com.mediflow.backend.repository.DoctorRepository;
import com.mediflow.backend.repository.SpecializationRepository;
import com.mediflow.backend.repository.UserRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDoctorService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final SpecializationRepository specializationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public record AdminDoctorResponse(
            @JsonUnwrapped DoctorResponse doctor,
            String email,
            String phoneNumber
    ) {}

    @Transactional
    public AdminDoctorResponse createDoctor(
            String authenticatedEmail, CreateDoctorRequest request) {

        User admin = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Login account not found"));

        if (admin.getRole() != UserRole.ADMIN
                || !Boolean.TRUE.equals(admin.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only an active admin can create a doctor");
        }

        var specialization = specializationRepository
                .findById(request.specializationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Specialization not found"));

        if (!Boolean.TRUE.equals(specialization.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Select an active specialization");
        }

        User doctorUser = new User();
        doctorUser.setFullName(request.fullName());
        doctorUser.setEmail(request.email());
        doctorUser.setPasswordHash(passwordEncoder.encode(request.temporaryPassword()));
        doctorUser.setPhoneNumber(request.phoneNumber());
        doctorUser.setRole(UserRole.DOCTOR);
        doctorUser.setActive(true);

        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);
        doctor.setSpecialization(specialization);
        doctor.setQualification(request.qualification());
        doctor.setLicenseNumber(request.licenseNumber());
        doctor.setExperienceYears(request.experienceYears());
        doctor.setConsultationFee(request.consultationFee());
        doctor.setConsultationDurationMinutes(request.consultationDurationMinutes());
        doctor.setBio(request.bio());
        doctor.setActive(true);

        if (request.languages() != null) {
            doctor.setLanguages(new ArrayList<>(request.languages()));
        }

        try {
            userRepository.saveAndFlush(doctorUser);
            doctorRepository.saveAndFlush(doctor);
        } catch (DataIntegrityViolationException ex) {
            // MySQL error 1062 means a unique value already exists.
            if (ex.getMostSpecificCause() instanceof SQLException sql
                    && sql.getErrorCode() == 1062) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Doctor email or license number already exists", ex);
            }
            throw ex;
        }

        AuditLog log = new AuditLog();
        log.setActorUser(admin);
        log.setAction("CREATE_DOCTOR");
        log.setEntityType("Doctor");
        log.setEntityId(doctor.getId());
        entityManager.persist(log);

        return new AdminDoctorResponse(
                DoctorResponse.from(doctor),
                doctorUser.getEmail(),
                doctorUser.getPhoneNumber());
    }
}