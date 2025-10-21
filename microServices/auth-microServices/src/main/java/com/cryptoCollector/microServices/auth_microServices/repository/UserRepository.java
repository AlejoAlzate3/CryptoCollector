package com.cryptoCollector.microServices.auth_microServices.repository;

import com.cryptoCollector.microServices.auth_microServices.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
