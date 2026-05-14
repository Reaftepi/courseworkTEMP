package kpi.pavlenko.shvets.coursework.repository;

import kpi.pavlenko.shvets.coursework.entity.AppStatus;
import kpi.pavlenko.shvets.coursework.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByStaffId(Long staffId);

    @Query("SELECT a FROM Appointment a WHERE a.staff.id = :staffId AND a.startTime >= :start AND a.startTime <= :end ORDER BY a.startTime")
    List<Appointment> findByStaffAndDay(@Param("staffId") Long staffId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<Appointment> findByStatus(AppStatus status);

    List<Appointment> findByPatientId(Long patientId);

    @Query("SELECT a FROM Appointment a WHERE a.startTime >= :start AND a.startTime < :end ORDER BY a.startTime")
    List<Appointment> findAllByDay(@Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);
}
