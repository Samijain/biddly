package com.biddly.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Compares with lower() on both sides, like the uq_users_email_lower index (R27). */
    boolean existsByEmailIgnoreCase(String email);
}
