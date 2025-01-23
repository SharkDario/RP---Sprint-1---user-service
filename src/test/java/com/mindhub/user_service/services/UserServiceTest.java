package com.mindhub.user_service.services;

import com.mindhub.user_service.dtos.NewUserDTO;
import com.mindhub.user_service.dtos.UpdateUserDTO;
import com.mindhub.user_service.models.EntityUser;
import com.mindhub.user_service.models.RoleType;
import com.mindhub.user_service.repositories.UserRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import jakarta.validation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

//import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Use @SpringBootTest to load the full Spring context and verify component integration
@SpringBootTest
@ActiveProfiles("test")
public class UserServiceTest {
    // Allows for creating and managing mocks of dependencies in unit tests,
    // facilitating the simulation of external components.
    // Simulacrum
    @MockBean
    private UserRepository userRepository;
    // Use @MockBean to replace real beans with mocks during testing, allowing you to focus on specific interactions.
    @Autowired
    private UserService userService;

    private EntityUser testUser;

    private Validator validator;

    @BeforeEach
    public void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        // Initialize the validator
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        //testUser = new EntityUser("Miguel7", encodedPassword, "miguel@gmail.com");
        testUser = spy(new EntityUser("Miguel7", "miguel@gmail.com", RoleType.USER));
        when(testUser.getId()).thenReturn(1L);
        // Mock the repository to return the test user when findByEmail is called
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("miguel@gmail.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("Miguel7")).thenReturn(testUser);
        when(userRepository.existsById(testUser.getId())).thenReturn(true);
        when(userRepository.existsByEmail("miguel@gmail.com")).thenReturn(true);
        when(userRepository.existsByUsername("Miguel7")).thenReturn(true);
        //userRepository = mock(userRepository.class);
    }

    @Test
    public void testGetEntityUserById() {
        // Mock the repository to return the test user when findById is called
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Call the service method
        EntityUser result = userService.getEntityUserById(1L);

        // Verify the result
        assertNotNull(result);
        assertEquals("Miguel7", result.getUsername());
        assertEquals("miguel@gmail.com", result.getEmail());

        // Verify that the repository method was called
        verify(userRepository, times(1)).findById(eq(1L));
    }

    @Test
    public void testGetEntityUserByIdNotFound() {
        // Mock the repository to return an empty optional when findById is called
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Verify that the service throws an exception when the user is not found
        assertThrows(RuntimeException.class, () -> userService.getEntityUserById(1L));

        // Verify that the repository method was called
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    public void testGetEntityUserByEmail() {
        // Call the service method
        EntityUser result = userService.getEntityUserByEmail("miguel@gmail.com");

        // Verify the result
        assertNotNull(result);
        assertEquals("Miguel7", result.getUsername());
        assertEquals("miguel@gmail.com", result.getEmail());

        // Verify that the repository method was called
        verify(userRepository, times(1)).findByEmail("miguel@gmail.com");
    }

    @Test
    public void testGetEntityUserByEmailNotFound() {
        // Mock the repository to return an empty optional when findByEmail is called
        when(userRepository.findByEmail("nonexistent@gmail.com")).thenReturn(Optional.empty());

        // Verify that the service throws an exception when the user is not found
        assertThrows(RuntimeException.class, () -> userService.getEntityUserByEmail("nonexistent@gmail.com"));

        // Verify that the repository method was called
        verify(userRepository, times(1)).findByEmail("nonexistent@gmail.com");
    }

    @Test
    public void testSaveEntityUser() {
        // Mock the repository to return the test user when save is called
        when(userRepository.save(any(EntityUser.class))).thenReturn(testUser);

        // Call the service method
        EntityUser result = userService.saveEntityUser(testUser);

        // Verify the result
        assertNotNull(result);
        assertEquals("Miguel7", result.getUsername());
        assertEquals("miguel@gmail.com", result.getEmail());

        // Verify that the repository method was called
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    public void testRegisterAdminUser() {
        // Mock the repository to return false when checking for existing email and username
        when(userRepository.existsByEmail("admin@gmail.com")).thenReturn(false);
        when(userRepository.existsByUsername("AdminUser")).thenReturn(false);

        // Mock the repository to return the test user when save is called
        when(userRepository.save(any(EntityUser.class))).thenReturn(testUser);

        // Call the service method
        NewUserDTO newUser = new NewUserDTO("AdminUser", "admin@gmail.com");
        userService.registerAdmin(newUser);

        // Verify that the repository methods were called
        verify(userRepository, times(1)).existsByEmail("admin@gmail.com");
        verify(userRepository, times(1)).existsByUsername("AdminUser");
        verify(userRepository, times(1)).save(any(EntityUser.class));
    }

    @Test
    public void testRegisterUser() {
        // Mock the repository to return false when checking for existing email and username
        when(userRepository.existsByEmail("user@gmail.com")).thenReturn(false);
        when(userRepository.existsByUsername("User")).thenReturn(false);

        // Mock the repository to return the test user when save is called
        when(userRepository.save(any(EntityUser.class))).thenReturn(testUser);

        // Call the service method
        NewUserDTO newUser = new NewUserDTO("User",  "user@gmail.com");
        userService.registerUser(newUser);

        // Verify that the repository methods were called
        verify(userRepository, times(1)).existsByEmail("user@gmail.com");
        verify(userRepository, times(1)).existsByUsername("User");
        verify(userRepository, times(1)).save(any(EntityUser.class));
    }

    @Test
    public void testValidateEntityUserEmailAlreadyInUse() {
        // Mock the repository to return true when checking for existing email
        when(userRepository.existsByEmail("existing@gmail.com")).thenReturn(true);

        // Verify that the service throws an exception when the email is already in use
        NewUserDTO newUser = new NewUserDTO("User", "existing@gmail.com");
        assertThrows(IllegalArgumentException.class, () -> userService.validateUser(newUser));

        // Verify that the repository method was called
        verify(userRepository, times(1)).existsByEmail("existing@gmail.com");
    }

    @Test
    public void testValidateEntityUserUsernameAlreadyInUse() {
        // Mock the repository to return true when checking for existing username
        when(userRepository.existsByUsername("ExistingUser")).thenReturn(true);

        // Verify that the service throws an exception when the username is already in use
        NewUserDTO newUser = new NewUserDTO("ExistingUser",  "user@gmail.com");
        assertThrows(IllegalArgumentException.class, () -> userService.validateUser(newUser));

        // Verify that the repository method was called
        verify(userRepository, times(1)).existsByUsername("ExistingUser");
    }

    @Test
    public void testUpdateEntityUserUsernameEmail() {
        // Mock the repository to return the test user when findById is called
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Mock the repository to return false when checking for existing email and username
        when(userRepository.existsByEmailAndIdNot("newemail@gmail.com", 1L)).thenReturn(false);
        when(userRepository.existsByUsernameAndIdNot("NewUsername", 1L)).thenReturn(false);

        // Call the service method
        UpdateUserDTO updatedUser = new UpdateUserDTO("NewUsername", "newemail@gmail.com");
        boolean result = userService.updateUser(1L, updatedUser);

        // Verify the result
        assertTrue(result);

        // Verify that the repository methods were called
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).existsByEmailAndIdNot("newemail@gmail.com", 1L);
        verify(userRepository, times(1)).existsByUsernameAndIdNot("NewUsername", 1L);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    public void testDeleteEntityUser() {
        // Mock the repository to return true when checking for existing user
        when(userRepository.existsById(1l)).thenReturn(true);

        assertTrue(userRepository.existsById(1L));
        // Call the service method
        boolean result = userService.deleteUser(1L);

        // Verify the result
        assertTrue(result);

        // Verify that the repository methods were called
        verify(userRepository, times(1)).existsById(1L);
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    public void testDeleteEntityUserNotFound() {
        // Mock the repository to return false when checking for existing user
        when(userRepository.existsById(eq(2L))).thenReturn(false);

        // Call the service method
        boolean result = userService.deleteUser(eq(2L));

        // Verify the result
        assertFalse(result);
    }
}
