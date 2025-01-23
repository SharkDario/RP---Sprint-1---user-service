package com.mindhub.user_service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mindhub.user_service.models.EntityUser;
import com.mindhub.user_service.models.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

// Data Transfer Object - transfer data with the back and front for example
public class UserDTO {
    // @JsonProperty - id in the response but not in the petition
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private final Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 10, message = "Username must be between 4 and 10 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    private RoleType role;

    // Constructor
    public UserDTO(EntityUser entityUser) {
        id = entityUser.getId();
        username = entityUser.getUsername();
        email = entityUser.getEmail();
        role = entityUser.getRole();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public RoleType getRole() {
        return role;
    }

}