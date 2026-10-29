package com.example.nosqllab2.repository;

import com.example.nosqllab2.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByValue(String value);

    boolean existsByValue(String value);
}
