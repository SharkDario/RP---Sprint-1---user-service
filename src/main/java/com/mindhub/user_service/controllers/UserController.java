package com.mindhub.user_service.controllers;

import com.mindhub.user_service.config.JwtUtils;
import com.mindhub.user_service.dtos.UpdateUserDTO;
import com.mindhub.user_service.dtos.UpdateUserPasswordDTO;
import com.mindhub.user_service.dtos.UserDTO;
import com.mindhub.user_service.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

// We use DTO to receive and send in controllers
@RestController
@RequestMapping("/api")
public class UserController {
    // Dependencies Injection - Only things that are in the context of Spring Boot (has to be Component)
    // From behind generates a constructor and injects the bean for this repository (interface)
    @Autowired
    private UserService userService; // inject the interface directly

    @Autowired
    private JwtUtils jwtUtils;

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

    // Endpoint to verify if a userId exists
    @GetMapping("/user/exists/{userId}")
    public ResponseEntity<Boolean> existsById(@PathVariable Long userId) {
        boolean exists = userService.existsById(userId);
        return ResponseEntity.ok(exists);
    }

    // if the user is authenticated: shows me the email
    @Operation(summary = "Get user's email (logged in)", description = "Return the email about the user authenticated")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User's email retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User's email not found"),
            @ApiResponse(responseCode = "401", description = "Without authorization")
    })
    @GetMapping("/user/email")
    public String getEmail(HttpServletRequest request){
        return jwtUtils.getEmailFromToken(request.getHeader("Authorization"));
    }

    // Return a user by authentication
    @Operation(summary = "Get user's information (logged in)", description = "Return the information about the user authenticated")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Without authorization"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/user/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        String email = getEmail(request);
        try {
            UserDTO user = userService.getUserDTOByEmail(email);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Update Username and Email from the authenticated user
    @Operation(summary = "Update user's information (logged in)", description = "Update your username and email")
    @ApiResponses(value={
            @ApiResponse(responseCode = "200", description = "User's username and email updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PutMapping("/user/profile")
    public ResponseEntity<?> updateProfile(HttpServletRequest request, @Valid @RequestBody UpdateUserDTO updatedUser) {
        try {
            Long userId = userService.getUserDTOByEmail(getEmail(request)).getId();
            userService.updateUser(userId, updatedUser);
            return new ResponseEntity<>("User updated successfully", HttpStatus.OK);
        } catch (IllegalArgumentException e) { // always before a RunTimeException that is general
            return new ResponseEntity<>("Invalid data provided: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // Update Password from the authenticated user
    @Operation(summary = "Update user's password (logged in)", description = "Update your password")
    @ApiResponses(value={
            @ApiResponse(responseCode = "200", description = "Your password has updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid password")
    })
    @PutMapping("/user/profile/password")
    public ResponseEntity<?> updatePassword(HttpServletRequest request, @Valid @RequestBody UpdateUserPasswordDTO updatedPassword) {
        try {
            Long userId = userService.getUserDTOByEmail(getEmail(request)).getId();
            userService.updateUserPassword(userId, updatedPassword);
            return new ResponseEntity<>("Password updated successfully", HttpStatus.OK);
        } catch (IllegalArgumentException e) { // always before a RunTimeException that is general
            return new ResponseEntity<>("Invalid data provided: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // Delete the authenticated user
    @Operation(summary = "Delete user (logged in)", description = "Delete my user that is authenticated")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Invalid input data"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @DeleteMapping("/user/delete")
    public ResponseEntity<?> deleteEntityUser(HttpServletRequest request) {
        Long userId = userService.getUserDTOByEmail(getEmail(request)).getId();
        boolean deleted = userService.deleteUser(userId);
        if (!deleted) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>("User deleted successfully", HttpStatus.OK);
    }

    @GetMapping("/internal/email/{email}")
    public ResponseEntity<Long> getByEmail(@PathVariable String email) throws EntityNotFoundException {
        Long userId = userService.getUserDTOByEmail(email).getId();
        return ResponseEntity.ok(userId);
    }
}