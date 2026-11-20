package com.mediflow.backend.features;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import com.mediflow.backend.dto.DoctorResponse;
import com.mediflow.backend.model.*;
import com.mediflow.backend.service.AdminDoctorService.AdminDoctorResponse;
import com.mediflow.backend.dto.SpecializationResponse;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import static com.mediflow.backend.features.ModuleAccess.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Transactional
public class AdminCatalogController {
    private final EntityManager em;
    private final ModuleAccess access;

    private static SpecializationResponse view(Specialization s) {
        return new SpecializationResponse(s.getId(), s.getName(), s.getDescription(), s.getActive());
    }

    public record NewSpec(@NotBlank @Size(max=100) String name, @Size(max=500) String description) {}
    public record SpecPatch(@Size(max=100) String name, @Size(max=500) String description, Boolean active) {}
    static AdminDoctorResponse view(Doctor d) {
        return new AdminDoctorResponse(DoctorResponse.from(d), d.getUser().getEmail(), d.getUser().getPhoneNumber());
    }

    public record DoctorPatch(@Size(min=2,max=100) String fullName,
            @Size(max=20) @Pattern(regexp="[+]?[0-9][0-9 ()-]{5,18}[0-9]") String phoneNumber,
            @Positive Long specializationId, @Size(max=255) String qualification,
            @Min(0) @Max(70) Integer experienceYears,
            @DecimalMin("0") @DecimalMax("100000") @Digits(integer=6,fraction=2) BigDecimal consultationFee,
            List<@NotBlank String> languages, @Size(max=2000) String bio, Boolean active) {}

    @GetMapping("/specializations")
    public List<SpecializationResponse> specs(Principal p) {
        access.require(p.getName(), UserRole.ADMIN);
        return em.createQuery("from Specialization order by name, id", Specialization.class)
                .getResultList().stream().map(AdminCatalogController::view).toList();
    }

    @PostMapping("/specializations")
    @ResponseStatus(HttpStatus.CREATED)
    public SpecializationResponse create(Principal p, @Valid @RequestBody NewSpec body) {
        var admin = access.require(p.getName(), UserRole.ADMIN);
        var s = new Specialization();
        s.setName(body.name().strip());
        s.setDescription(body.description());
        em.persist(s);
        access.audit(admin, "CREATE_SPECIALIZATION", "Specialization", s.getId());
        return view(s);
    }

    @PatchMapping("/specializations/{id}")
    public SpecializationResponse modify(Principal p, @PathVariable long id, @Valid @RequestBody SpecPatch body) {
        var admin = access.require(p.getName(), UserRole.ADMIN);
        var s = access.locked(Specialization.class, id);
        if (body.name() != null) {
            if (body.name().isBlank()) throw invalid("Name is required");
            s.setName(body.name().strip());
        }
        if (body.description() != null) s.setDescription(body.description());
        if (body.active() != null) s.setActive(body.active());
        access.audit(admin, "UPDATE_SPECIALIZATION", "Specialization", id);
        em.flush();
        return view(s);
    }

    @PatchMapping("/doctors/{id}")
    public AdminDoctorResponse doctor(Principal p, @PathVariable long id, @Valid @RequestBody DoctorPatch body) {
        var admin = access.require(p.getName(), UserRole.ADMIN);
        var d = access.locked(Doctor.class, id);
        if (body.fullName() != null) {
            if (body.fullName().strip().length() < 2) throw invalid("Enter the doctor's full name");
            d.getUser().setFullName(body.fullName().strip());
        }
        if (body.specializationId() != null) {
            var s = access.locked(Specialization.class, body.specializationId());
            if (!s.getActive()) throw invalid("Choose an active specialization");
            d.setSpecialization(s);
        }
        if (body.qualification() != null) {
            if (body.qualification().isBlank()) throw invalid("Qualification is required");
            d.setQualification(body.qualification().strip());
        }
        if (body.phoneNumber() != null) d.getUser().setPhoneNumber(body.phoneNumber());
        if (body.experienceYears() != null) d.setExperienceYears(body.experienceYears());
        if (body.consultationFee() != null) d.setConsultationFee(body.consultationFee());
        if (body.languages() != null) d.setLanguages(body.languages());
        if (body.bio() != null) d.setBio(body.bio());
        if (body.active() != null) d.setActive(body.active());
        access.audit(admin, "UPDATE_DOCTOR", "Doctor", id);
        return view(d);
    }
}
