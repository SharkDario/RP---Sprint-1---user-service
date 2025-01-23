package com.mindhub.user_service.repositories;

import com.mindhub.user_service.models.EntityUser;
import com.mindhub.user_service.models.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// <class EntityUser, ID's type
// @Repository - It's not necessary because JpaRepository already have @Repository from its extensions
// a Repository is a component, that moves the information to the DB and brings it from the DB
// <Generic>
public interface UserRepository extends JpaRepository<EntityUser, Long> {
    EntityUser findByUsername(String username);
    Optional<EntityUser> findByEmail(String email);
    List<EntityUser> findByRole(RoleType role);

    boolean existsById(long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);;
    // Update validation
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByUsernameAndIdNot(String username, Long id);

    int countById(long id);
    int countByRole(RoleType role);
    int countByUsername(String username);
    int countByEmail(String email);

    // Pagination example
    Page<EntityUser> findByRole(RoleType role, Pageable pageable);
}