package com.mindhub.user_service.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindhub.user_service.dtos.UpdateUserDTO;
import com.mindhub.user_service.dtos.UserDTO;
import com.mindhub.user_service.models.EntityUser;
import com.mindhub.user_service.models.RoleType;
import com.mindhub.user_service.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// This annotation is used to test Spring MVC controllers,
// focusing only on the web layer
@WebMvcTest(UserController.class)
public class UserControllerTest {
    // Autowired to inject MockMvc for simulating HTTP requests
    @Autowired
    private MockMvc mockMvc;
    // MockBean to mock the EntityUserService dependency
    @MockBean
    private UserService userService;
    // Autowired to inject ObjectMapper for JSON serialization/deserialization
    @Autowired
    private ObjectMapper objectMapper;
    // Test user DTO object to be used in tests
    private UserDTO testUser;
    // Constant for the test user's email
    private final String EMAIL = "dario@gmail.com";
    // This method runs before each test to set up initial data
    @BeforeEach
    void setUp() {
        // Create a test user and its DTO
        EntityUser user = new EntityUser("Dario7",  EMAIL, RoleType.USER);
        testUser = new UserDTO(user);
    }
    // Test to verify that the /api/user/profile endpoint returns the user's profile
    @Test
    void getProfileShouldReturnUserProfile() throws Exception {
        // Mock the service to return the test user DTO
        when(userService.getUserDTOByEmail(EMAIL)).thenReturn(testUser);

        mockMvc.perform(get("/api/user/profile/" + EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId())) // Verify the ID matches
                .andExpect(jsonPath("$.username").value(testUser.getUsername())) // Verify the username matches
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.role").value(testUser.getRole().toString()));
    }

    @Test
    void updateProfileShouldUpdateUserProfile() throws Exception {
        // Create a DTO with updated username and email
        UpdateUserDTO updateDto = new UpdateUserDTO("newname", "newemail@example.com");
        // Mock the service to return the test user and confirm the update
        when(userService.getUserDTOByEmail(EMAIL)).thenReturn(testUser);
        when(userService.updateUser(anyLong(), any(UpdateUserDTO.class))).thenReturn(true);

        mockMvc.perform(patch("/api/user/profile/1")
                        .contentType(MediaType.APPLICATION_JSON) // Set content type to JSON
                        .content(objectMapper.writeValueAsString(updateDto))) // Convert DTO to JSON
                .andExpect(status().isOk()) // Expect HTTP 200 status
                .andExpect(content().string("User updated successfully")); // Expected message
    }
}
