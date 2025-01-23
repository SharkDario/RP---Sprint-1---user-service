package com.mindhub.user_service.services.impl;

import com.mindhub.user_service.dtos.NewUserDTO;
import com.mindhub.user_service.dtos.UpdateUserDTO;
import com.mindhub.user_service.dtos.UserDTO;
import com.mindhub.user_service.models.EntityUser;
import com.mindhub.user_service.models.RoleType;
import com.mindhub.user_service.repositories.UserRepository;
import com.mindhub.user_service.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

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
    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }
}
