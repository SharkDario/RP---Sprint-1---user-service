package com.mindhub.user_service.repositories;
import com.mindhub.user_service.models.EntityUser;

import com.mindhub.user_service.models.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

// This annotation is used for JPA tests, it configures an in-memory database and JPA repositories
@DataJpaTest
public class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    private EntityUser user;

    @BeforeEach
    public void setUp(){
        // Create a user
        user = new EntityUser();
        user.setUsername("Dario7");
        user.setEmail("dario@gmail.com");
        user.setRole(RoleType.USER);
        userRepository.save(user);
    }

    @Test
    public void testCreateNewUser() {
        EntityUser newUser = new EntityUser();
        newUser.setUsername("Franco7");
        newUser.setEmail("franco@gmail.com");
        newUser.setRole(RoleType.USER);
        EntityUser userToSave = userRepository.save(newUser);
        assertNotNull(userToSave.getId());
        assertEquals("Franco7", userToSave.getUsername());
    }

    @Test
    public void testUpdateUser() {
        EntityUser foundUser = userRepository.findById(user.getId()).orElse(null);
        assertThat(foundUser).isNotNull();

        foundUser.setUsername("Dario8");
        userRepository.save(foundUser);

        EntityUser updatedUser = userRepository.findById(user.getId()).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getUsername()).isEqualTo("Dario8");
    }

    @Test
    public void testDeleteUser() {
        userRepository.deleteById(user.getId());
        boolean exists = userRepository.existsById(user.getId());
        assertFalse(exists);
    }
}
