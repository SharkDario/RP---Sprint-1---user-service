package com.mindhub.user_service.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindhub.user_service.dtos.NewEntityUser;
import com.mindhub.user_service.dtos.NewUserDTO;
import com.mindhub.user_service.dtos.UpdateUserDTO;
import com.mindhub.user_service.dtos.UserDTO;
import com.mindhub.user_service.models.RoleType;
import com.mindhub.user_service.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import javax.management.relation.Role;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// Producer
// We use DTO to receive and send in controllers
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    // Dependencies Injection - Only things that are in the context of Spring Boot (has to be Component)
    // From behind generates a constructor and injects the bean for this repository (interface)
    @Autowired
    private UserService userService; // inject the interface directly

    // Validate errors
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }
    // Validate business exceptions
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(EntityNotFoundException.class)
    public Map<String, String> handleEntityNotFound(EntityNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return error;
    }

    // Validate general exceptions
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Map<String, String> handleGeneralExceptions(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "An unexpected error occurred: " + ex.getMessage());
        return error;
    }

    // List all users
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllEntityUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    // List all roles
    @GetMapping("/roles")
    public ResponseEntity<List<RoleType>> getAllRoles() {
        List<RoleType> roles = Arrays.asList(RoleType.values());
        return new ResponseEntity<>(roles, HttpStatus.OK);
    }

    // Return a user by id
    @GetMapping("/user/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            UserDTO user = userService.getUserDTOById(id);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Create a user
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@Valid @RequestBody NewEntityUser newUser) {
        userService.registerUser(newUser);
        //userService.sendWelcomeEmailAuth(newUser);
        return new ResponseEntity<>("User created successfully", HttpStatus.CREATED);
    }

    // Create a ADMIN user
    @PostMapping("/admins")
    public ResponseEntity<?> createAdmin(@Valid @RequestBody NewEntityUser newAdmin) {
        userService.registerAdmin(newAdmin);
        return new ResponseEntity<>("Admin created successfully", HttpStatus.CREATED);
    }

    // Update Username and Email - User
    @PatchMapping("/user/{id}")
    public ResponseEntity<?> updateEntityUser(@PathVariable Long id, @Valid @RequestBody UpdateUserDTO updateUser) {
        // for testing
        //amqpTemplate.convertAndSend("testingExchange", "routing.key2", id);
        try {
            userService.updateUser(id, updateUser);
            return new ResponseEntity<>("User updated successfully", HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Delete a user
    @DeleteMapping("/user/{id}")
    public ResponseEntity<?> deleteEntityUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);
        if (!deleted) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>("User deleted successfully", HttpStatus.OK);
    }

    // Endpoint to return the userId by the email
    @GetMapping("/email/{email}")
    public ResponseEntity<Long> getByEmail(@PathVariable String email) throws EntityNotFoundException {
        Long userId = userService.getUserDTOByEmail(email).getId();
        return ResponseEntity.ok(userId);
    }
}