package com.example.rag.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserStore store;
    private final LoginAttemptService attempts;

    public AppUserDetailsService(UserStore store, LoginAttemptService attempts) {
        this.store = store;
        this.attempts = attempts;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserStore.UserRecord u = store.find(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return User.withUsername(u.username())
                .password(u.passwordHash())
                .roles("USER")
                .accountLocked(attempts.isLocked(u.username()))
                .build();
    }
}
