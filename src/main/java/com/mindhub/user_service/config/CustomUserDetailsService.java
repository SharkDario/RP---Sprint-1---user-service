package com.mindhub.user_service.config;

import com.mindhub.user_service.models.EntityUser;
import com.mindhub.user_service.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository entityUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Spring Security: how to identify the user by email
        EntityUser userEntity = entityUserRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        // finds or not in the db
        // create the authentication - new=constructor - spring security validates the password with the password sent
        return new User(userEntity.getEmail(), userEntity.getPassword(), AuthorityUtils.createAuthorityList
                (userEntity.getRole().toString()));
    }
}
