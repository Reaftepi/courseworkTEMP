package kpi.pavlenko.shvets.coursework.repository;

import kpi.pavlenko.shvets.coursework.entity.SessionNotes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionNotesRepository extends JpaRepository<SessionNotes, Long> {
    List<SessionNotes> findByAppointmentId(Long appointmentId);
}
