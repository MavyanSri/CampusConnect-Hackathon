package com.mediflow.backend.features;

import java.time.ZoneId;
import com.mediflow.backend.model.*;
import com.mediflow.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
class ModuleAccess {
    static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private final EntityManager em;
    private final UserRepository users;

    User require(String email, UserRole role) {
        User actor = users.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
        if (actor.getRole() != role || !Boolean.TRUE.equals(actor.getActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Active " + role + " account required");
        }
        return actor;
    }

    Doctor doctor(String email, boolean lock) {
        User actor = require(email, UserRole.DOCTOR);
        var query = em.createQuery("select d from Doctor d where d.user.id = :id and d.active = true", Doctor.class)
                .setParameter("id", actor.getId());
        if (lock) query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        return query.getResultList().stream().findFirst().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Active doctor profile required"));
    }

    <T> T owned(Class<T> type, long id, Doctor doctor) {
        return em.createQuery("select e from " + type.getSimpleName()
                        + " e where e.id = :id and e.doctor.id = :doctor", type)
                .setParameter("id", id).setParameter("doctor", doctor.getId())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList().stream().findFirst()
                .orElseThrow(() -> missing("Record"));
    }

    <T> T locked(Class<T> type, long id) {
        T record = em.find(type, id, LockModeType.PESSIMISTIC_WRITE);
        if (record == null) throw missing(type.getSimpleName());
        return record;
    }

    void audit(User actor, String action, String type, long id) {
        var log = new AuditLog();
        log.setActorUser(actor);
        log.setAction(action);
        log.setEntityType(type);
        log.setEntityId(id);
        em.persist(log);
    }

    static ResponseStatusException missing(String name) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, name + " not found");
    }

    static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
