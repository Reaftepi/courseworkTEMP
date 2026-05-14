package kpi.pavlenko.shvets.coursework.repository;

import kpi.pavlenko.shvets.coursework.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByUserId(Long userID);
    List<Staff> findByIsMedicalTrue();
    List<Staff> findBySectionId(Long id);
}
