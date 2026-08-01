package com.aiservice.platform.identity.service.impl;

import com.aiservice.platform.identity.entity.User;
import com.aiservice.platform.identity.exception.UserNotFoundException;
import com.aiservice.platform.identity.repository.UserRepository;
import com.aiservice.platform.identity.security.CustomUserDetails;
import com.aiservice.platform.identity.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsServiceImpl implements CustomUserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with username: " + username
                        ));

        return new CustomUserDetails(
                user,
                Collections.emptySet()
        );
    }
}