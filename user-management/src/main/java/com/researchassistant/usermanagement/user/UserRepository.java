package com.researchassistant.usermanagement.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Exact match, not IgnoreCase: emails are normalised to lowercase before
     * being stored, so this lines up with the unique index on the column.
     */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
