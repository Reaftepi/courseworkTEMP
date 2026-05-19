package kpi.pavlenko.shvets.coursework.repository;

import kpi.pavlenko.shvets.coursework.entity.Role;
import kpi.pavlenko.shvets.coursework.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);
    List<User> findByRole(Role role);
    boolean existsByLogin(String login);
    Optional<User> findByResetToken(String resetToken);
}
