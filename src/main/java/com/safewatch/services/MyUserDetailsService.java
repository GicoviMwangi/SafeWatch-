package com.safewatch.services;

import com.safewatch.models.User;
import com.safewatch.repositories.CurrentUserRepository;
import com.safewatch.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyUserDetailsService implements UserDetailsService {
    private final CurrentUserRepository currentUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = currentUserRepository.findByEmail(username).orElseThrow();

        return new UserPrincipal(user);
    }
}
