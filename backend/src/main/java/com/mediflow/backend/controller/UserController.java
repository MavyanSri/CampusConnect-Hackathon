package com.mediflow.backend.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mediflow.backend.model.Gender;
import com.mediflow.backend.model.User;
import com.mediflow.backend.model.UserRole;
import com.mediflow.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    public record UserProfileResponse(
            Long id,
            String fullName,
            String email,
            String phoneNumber,
            Gender gender,
            LocalDate dateOfBirth,
            String profileImageUrl,
            UserRole role,
            Boolean active,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime createdAt
    ) {
        public static UserProfileResponse from(User user) {
            return new UserProfileResponse(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhoneNumber(),
                    user.getGender(),
                    user.getDateOfBirth(),
                    user.getProfileImageUrl(),
                    user.getRole(),
                    user.getActive(),
                    user.getCreatedAt()
            );
        }
    }

    @GetMapping("/me")
    public UserProfileResponse getMyProfile(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return UserProfileResponse.from(user);
    }

    @PatchMapping("/me")
    @Transactional
    public UserProfileResponse updateMyProfile(
            Authentication authentication,
            @RequestBody Map<String, Object> changes) {

        User user = getCurrentUser(authentication);

        for (var entry : changes.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            if (value != null && !(value instanceof String)) {
                throw badRequest(field + " must be a string or null");
            }

            String text = value == null ? null : ((String) value).strip();

            switch (field) {
                case "fullName" -> {
                    if (text == null || text.length() < 2 || text.length() > 100) {
                        throw badRequest("Full name must be 2 to 100 characters");
                    }
                    user.setFullName(text);
                }

                case "phoneNumber" -> {
                    if (text == null || text.isEmpty()) {
                        user.setPhoneNumber(null);
                    } else {
                        if (text.length() > 20
                                || !text.matches("\\+?[0-9][0-9 ()-]{5,18}[0-9]")) {
                            throw badRequest("Invalid phone number");
                        }
                        user.setPhoneNumber(text);
                    }
                }

                case "gender" -> {
                    try {
                        user.setGender(text == null ? null : Gender.valueOf(text));
                    } catch (IllegalArgumentException ex) {
                        throw badRequest("Invalid gender value");
                    }
                }

                case "dateOfBirth" -> {
                    try {
                        LocalDate dob = text == null ? null : LocalDate.parse(text);
                        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

                        if (dob != null && dob.isAfter(today)) {
                            throw badRequest("Date of birth cannot be in the future");
                        }
                        user.setDateOfBirth(dob);
                    } catch (DateTimeParseException ex) {
                        throw badRequest("Date of birth must use yyyy-MM-dd");
                    }
                }

                default -> throw badRequest("Field cannot be updated: " + field);
            }
        }

        return UserProfileResponse.from(user);
    }

    private User getCurrentUser(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Login account not found"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Account is inactive");
        }

        return user;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}