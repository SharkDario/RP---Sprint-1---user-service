package com.mindhub.user_service.services.impl;

import com.mindhub.user_service.config.JwtUtils;

import com.mindhub.user_service.dtos.*;
import com.mindhub.user_service.exceptions.UserException;
import com.mindhub.user_service.models.EntityUser;
import com.mindhub.user_service.models.RoleType;
import com.mindhub.user_service.models.Status;
import com.mindhub.user_service.repositories.UserRepository;
import com.mindhub.user_service.services.UserService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Value("${jwt.verifyUserKey}")
    private String verifyUserKey; //"a2lhc2hqa2ZhamxrZ2xrc2FqbGtzYWpsZ2xrYXNkamxrZ2xrYXNsa3NhbGtqZ2xrc2Fsa2RqZ2Zsa2FzamRzYWxramdsa2FzZA";

    // Inject the AmqpTemplate bean to send messages to RabbitMQ
    // user-service is the Producer/Publisher
    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private JwtUtils jwtUtil;

    @Override
    public UserDTO getUserDTOById(Long id) {
        return new UserDTO(getEntityUserById(id));
    }

    @Override
    public UserDTO getUserDTOByEmail(String email) {
        return new UserDTO(getEntityUserByEmail(email));
    }

    @Override
    public EntityUser getEntityUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User with ID " + id + " not found"));
    }

    @Override
    public EntityUser getEntityUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User with email " + email + " not found"));
    }

    @Override
    public EntityUser saveEntityUser(EntityUser entityUser) {
        return userRepository.save(entityUser);
    }

    @Override
    public void registerAdmin(NewUserDTO newAdminDTO) {
        validateUser(newAdminDTO);
        EntityUser entityUser = new EntityUser(newAdminDTO.username(), newAdminDTO.email(), RoleType.ADMIN);
        saveEntityUser(entityUser);
    }

    @Override
    public void registerUser(NewUserDTO newUserDTO) {
        validateUser(newUserDTO);
        EntityUser entityUser = new EntityUser(newUserDTO.username(), newUserDTO.email(), RoleType.USER);
        saveEntityUser(entityUser);
    }


    @Override
    public List<UserDTO> getAllEntityUsers() {
        return userRepository.findAll().stream()
                .map(UserDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Override
    public boolean updateUser(Long id, UpdateUserDTO updateUserDTO) {
        EntityUser entityUser = getEntityUserById(id);
        // Validate unique email and username
        if(!updateUserDTO.email().equals(entityUser.getEmail()) &&
                userRepository.existsByEmailAndIdNot(updateUserDTO.email(), id)) {
            throw new IllegalArgumentException("The email " + updateUserDTO.email() + " is already in use.");
        }
        if(!updateUserDTO.username().equals(entityUser.getUsername()) &&
                userRepository.existsByUsernameAndIdNot(updateUserDTO.username(), id)) {
            throw new IllegalArgumentException("The username " + updateUserDTO.username() + " is already in use.");
        }
        // Update the user
        entityUser.setUsername(updateUserDTO.username());
        entityUser.setEmail(updateUserDTO.email());
        saveEntityUser(entityUser);
        return true;
    }

    @Override
    public boolean deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }
        userRepository.deleteById(id);
        return true;
    }

    @Override
    public void validateUser(NewUserDTO newUserDTO) {
        // Validate unique email
        if (userRepository.existsByEmail(newUserDTO.email())) {
            throw new IllegalArgumentException("The email " + newUserDTO.email() + " is already in use.");
        }
        // Validate unique username
        if (userRepository.existsByUsername(newUserDTO.username())) {
            throw new IllegalArgumentException("The username " + newUserDTO.username() + " is already in use.");
        }
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void verifyUser(Long id) throws UserException {
        EntityUser user = userRepository.findById(id).orElseThrow(()->new UserException("The user doesn't exists", HttpStatus.NOT_FOUND));
        user.setStatus(Status.ACTIVE);
        userRepository.save(user);
    }

    @Override
    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    public void validateEntityUser(NewEntityUser newEntityUser) {
        // Validate unique email
        if (userRepository.existsByEmail(newEntityUser.email())) {
            throw new IllegalArgumentException("The email " + newEntityUser.email() + " is already in use.");
        }
        // Validate unique username
        if (userRepository.existsByUsername(newEntityUser.username())) {
            throw new IllegalArgumentException("The username " + newEntityUser.username() + " is already in use.");
        }
    }

    @Override
    public void registerAdmin(NewEntityUser newEntityUser) {
        validateEntityUser(newEntityUser);
        EntityUser entityUser = new EntityUser(newEntityUser.username(), newEntityUser.email(), RoleType.ADMIN);
        entityUser.setPassword(passwordEncoder.encode(newEntityUser.password()));
        entityUser.setStatus(Status.ACTIVE);
        saveEntityUser(entityUser);
    }

    @Override
    public void registerUser(NewEntityUser newEntityUser) {
        validateEntityUser(newEntityUser);
        EntityUser entityUser = new EntityUser(newEntityUser.username(), newEntityUser.email(), RoleType.USER);
        entityUser.setPassword(passwordEncoder.encode(newEntityUser.password()));
        // This only is active when you don't want to send an email with the token registration:
        // entityUser.setStatus(Status.ACTIVE);
        saveEntityUser(entityUser);
        sendWelcomeEmailAuth(entityUser);
    }

    @Override
    public String loginUser(LoginRequest loginRequest) throws UserException {
        try {
            UserDTO userDTO = getUserDTOByEmail(loginRequest.email());

            // pass: csrf, authorizedHttpRequests (USER), cors (* any frontend), jwtAuthenticationFilter (doFilterInternal URI, extract Authorization, Extract token, jwlUtils extract subject
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(// use the CustomUserDetailsService because is using the UserDetails
                            loginRequest.email(),
                            loginRequest.password()
                    )
            );
            // set the authentication
            SecurityContextHolder.getContext().setAuthentication(authentication);

            if (userDTO.getStatus() == Status.ACTIVE) {
                // generate the token  // return the token
                return jwtUtil.generateToken(authentication.getName(), userDTO.getId(), userDTO.getRole().toString());
            } else {
                throw new UserException("You need to verify your email before sign up", HttpStatus.UNAUTHORIZED);
            }
            //return jwtUtil.generateToken(authentication.getName(), userDTO.getId(), userDTO.getRole().toString());
        } catch (BadCredentialsException e) {
            throw new UserException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }
    }

    private void sendWelcomeEmailAuth(EntityUser newEntityUser) {
        String jwt = jwtUtil.generateRegisterToken(newEntityUser.getId(),50000L, verifyUserKey);
        WelcomeMessage message = new WelcomeMessage(newEntityUser.getUsername(), newEntityUser.getEmail(), jwt);
        amqpTemplate.convertAndSend("testingExchange", "routingUserRegister.key", message);
        // Send a message to RabbitMQ using the AmqpTemplate
        // The message is sent to the "testingExchange" with the routing key "routingUserRegister.key"
    }

    @Override
    public Long getAuthenticatedUserId(Authentication authentication) {
        String token = (String) authentication.getCredentials();
        return jwtUtil.extractId(token);
    }

    @Override
    public boolean updateUserPassword(Long id, UpdateUserPasswordDTO updatedPassword) {
        EntityUser entityUser = getEntityUserById(id);
        // passwordEncoder.matches() to validate the old password
        if (!passwordEncoder.matches(updatedPassword.oldPassword(), entityUser.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        // Encode the new password before saving
        entityUser.setPassword(passwordEncoder.encode(updatedPassword.newPassword()));
        userRepository.save(entityUser);
        return true;
    }
}
