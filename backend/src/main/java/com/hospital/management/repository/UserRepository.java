package com.hospital.management.repository;

import com.hospital.management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.role = :role AND NOT EXISTS "
            + "(SELECT d.id FROM Doctor d WHERE d.user.id = u.id) ORDER BY u.lastName, u.firstName")
    List<User> findUsersWithoutDoctorProfile(@Param("role") User.Role role);
}