package com.mindhub.user_service.services;

import com.mindhub.user_service.dtos.NewUserDTO;
import com.mindhub.user_service.dtos.UpdateUserDTO;
import com.mindhub.user_service.dtos.UserDTO;
import com.mindhub.user_service.models.EntityUser;

import java.util.List;

public interface UserService {
    // only declare methods because it's an interface
    UserDTO getUserDTOById(Long id);

    UserDTO getUserDTOByEmail(String email);

    EntityUser getEntityUserById(Long id);

    EntityUser getEntityUserByEmail(String email);

    EntityUser saveEntityUser(EntityUser entityUser);

    void registerAdmin(NewUserDTO newAdminDTO);

    void registerUser(NewUserDTO newUserDTO);

    public List<UserDTO> getAllEntityUsers();

    public boolean existsByEmail(String email);

    boolean updateUser(Long id, UpdateUserDTO updateUserDTO);

    public boolean deleteUser(Long id);

    public void validateUser(NewUserDTO newUserDTO);

    public boolean existsById(Long id);
}
