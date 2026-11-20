
package com.mediflow.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "doctors",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_doctors_user",
            columnNames = "user_id"
        ),
        @UniqueConstraint(
            name = "uk_doctors_license",
            columnNames = "license_number"
        )
    }
)
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialization_id", nullable = false)
    private Specialization specialization;

    @Column(name = "qualification", nullable = false, length = 255)
    private String qualification;

    @Column(name = "license_number", nullable = false, length = 100)
    private String licenseNumber;

    @Column(name = "experience_years", nullable = false)
    private Integer experienceYears = 0;

    @Column(
        name = "consultation_fee",
        nullable = false,
        precision = 10,
        scale = 2
    )
    private BigDecimal consultationFee;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "languages", columnDefinition = "json")
    private List<String> languages = new ArrayList<>();

    @Column(name = "consultation_duration_minutes", nullable = false)
    private Integer consultationDurationMinutes = 30;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Doctor() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));

        createdAt = now;
        updatedAt = now;

        if (experienceYears == null) {
            experienceYears = 0;
        }

        if (consultationDurationMinutes == null) {
            consultationDurationMinutes = 30;
        }

        if (active == null) {
            active = true;
        }

        if (languages == null) {
            languages = new ArrayList<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
    }
}
