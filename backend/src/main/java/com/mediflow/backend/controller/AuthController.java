package com.mediflow.backend.controller;

import java.time.Instant;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.mediflow.backend.config.SecurityConfig;
import com.mediflow.backend.model.User;
import com.mediflow.backend.model.UserRole;
import com.mediflow.backend.repository.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 100) String fullName,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 20)
            @Pattern(regexp = "\\+?[0-9][0-9 ()-]{5,18}[0-9]")
            String phoneNumber
    ) {
        public RegisterRequest {
            fullName = fullName == null ? null : fullName.strip();
            email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
            phoneNumber = phoneNumber == null || phoneNumber.isBlank()
                    ? null : phoneNumber.strip();
        }
    }

    public record RegisterResponse(
            Long id,
            String fullName,
            String email,
            String phoneNumber,
            UserRole role
    ) {}

    public record LoginRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank String password
    ) {
        public LoginRequest {
            email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
        }
    }

    public record LoginUser(
            Long id,
            String fullName,
            String email,
            UserRole role
    ) {}

    public record LoginResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            LoginUser user
    ) {}

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhoneNumber(request.phoneNumber());
        user.setRole(UserRole.PATIENT);
        user.setActive(true);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            if (userRepository.findByEmail(request.email()).isPresent()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Email already registered");
            }
            throw ex;
        }

        return new RegisterResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getActive())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        Instant now = Instant.now();
        long expiresIn = 3600;

        var claims = JwtClaimsSet.builder()
                .issuer(SecurityConfig.JWT_ISSUER)
                .subject(user.getEmail())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .build();

        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)).getTokenValue();

        var loginUser = new LoginUser(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole());

        return new LoginResponse(accessToken, "Bearer", expiresIn, loginUser);
    }
}