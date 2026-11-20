package com.mediflow.backend.features;

import java.security.Principal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import com.mediflow.backend.model.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mediflow.backend.service.AdminDoctorService.AdminDoctorResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import static com.mediflow.backend.features.ModuleAccess.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class AdminReportsController {
    private final ModuleAccess access;
    private final EntityManager em;
    private final JdbcTemplate jdbc;

    public record Result<T>(List<T> content, int page, int size, long totalElements, long totalPages) {}
    public record PatientView(Long id, String fullName, String email, String phoneNumber,
                              boolean active, LocalDateTime createdAt) {}

    public record BookingView(Long id, String patientName, Long doctorId, String doctorName,
            String specializationName, @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTime,
            @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endTime, AppointmentStatus status,
            @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt) {
        static BookingView from(Appointment a) {
            var doctor = a.getDoctor();
            return new BookingView(a.getId(), a.getPatient().getFullName(), doctor.getId(),
                    doctor.getUser().getFullName(), doctor.getSpecialization().getName(),
                    a.getStartTime(), a.getEndTime(), a.getStatus(), a.getCreatedAt());
        }
    }

    private <T,R> Result<R> list(Class<T> type, String from, String order, Map<String,Object> params,
            int page, int size, Function<T,R> map) {
        long offset = (long) page * size;
        if (page < 0 || size < 1 || size > 50 || offset > Integer.MAX_VALUE) throw invalid("Check page and size (1–50)");
        var count = em.createQuery("select count(e) " + from, Long.class);
        var rows = em.createQuery("select e " + from + " order by " + order, type);
        params.forEach((k,v) -> { count.setParameter(k,v); rows.setParameter(k,v); });
        long total = count.getSingleResult();
        var data = rows.setFirstResult((int) offset).setMaxResults(size).getResultList();
        return new Result<>(data.stream().map(map).toList(), page, size, total, (total + size - 1) / size);
    }

    @GetMapping("/doctors")
    public Result<AdminDoctorResponse> doctors(Principal p, @RequestParam(defaultValue="") String search,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size) {
        access.require(p.getName(), UserRole.ADMIN);
        if (search.length() > 100) throw invalid("Search is too long");
        return list(Doctor.class, "from Doctor e where locate(lower(:q),lower(e.user.fullName)) > 0",
                "e.user.fullName, e.id", Map.of("q",search.strip()), page,size,AdminCatalogController::view);
    }

    @GetMapping("/patients")
    public Result<PatientView> patients(Principal p, @RequestParam(defaultValue="0") int page,
                                       @RequestParam(defaultValue="10") int size) {
        access.require(p.getName(), UserRole.ADMIN);
        return list(User.class, "from User e where e.role = :role", "e.id", Map.of("role",UserRole.PATIENT),
                page,size,u -> new PatientView(u.getId(),u.getFullName(),u.getEmail(),u.getPhoneNumber(),u.getActive(),u.getCreatedAt()));
    }

    @GetMapping("/appointments")
    public Result<BookingView> appointments(Principal p, @RequestParam(required=false) AppointmentStatus status,
            @RequestParam(required=false) Long doctorId, @RequestParam(required=false) LocalDate date,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size) {
        access.require(p.getName(), UserRole.ADMIN);
        var from = new StringBuilder("from Appointment e where 1=1");
        Map<String,Object> params = new HashMap<>();
        if (status != null) { from.append(" and e.status=:status"); params.put("status",status); }
        if (doctorId != null) { from.append(" and e.doctor.id=:doctor"); params.put("doctor",doctorId); }
        if (date != null) {
            from.append(" and e.startTime >= :start and e.startTime < :end");
            params.put("start",date.atStartOfDay());
            params.put("end",date.plusDays(1).atStartOfDay());
        }
        return list(Appointment.class, from.toString(), "e.startTime desc, e.id desc", params,page,size,BookingView::from);
    }

    @GetMapping("/dashboard")
    public Map<String,Object> dashboard(Principal p) {
        access.require(p.getName(), UserRole.ADMIN);
        var today = LocalDate.now(ZONE);
        return jdbc.queryForMap("""
                select (select count(*) from users where role='PATIENT') as totalPatients,
                       (select count(*) from doctors) as totalDoctors,
                       (select count(*) from doctors d join users u on u.id=d.user_id where d.active=true and u.active=true) as activeDoctors,
                       count(*) as totalAppointments,
                       count(case when start_time>=? and start_time<? then 1 end) as todayAppointments,
                       count(case when status='CONFIRMED' then 1 end) as confirmedAppointments,
                       count(case when status='COMPLETED' then 1 end) as completedAppointments,
                       count(case when status='CANCELLED' then 1 end) as cancelledAppointments
                from appointments
                """,today.atStartOfDay(),today.plusDays(1).atStartOfDay());
    }

    @GetMapping("/analytics")
    public Map<String,List<Map<String,Object>>> analytics(Principal p) {
        access.require(p.getName(), UserRole.ADMIN);
        return Map.of("appointmentsByMonth",jdbc.queryForList("""
                    select date_format(start_time,'%Y-%m') as month, count(*) as count
                    from appointments group by date_format(start_time,'%Y-%m') order by month
                    """),
                "appointmentsByStatus",jdbc.queryForList("select status, count(*) as count from appointments group by status order by status"),
                "appointmentsBySpecialization",jdbc.queryForList("""
                    select s.name as specializationName, count(*) as count from appointments a
                    join doctors d on d.id=a.doctor_id join specializations s on s.id=d.specialization_id
                    group by s.id,s.name order by count desc,s.name
                    """));
    }
}
