package com.datalabeling.datalabelingsupportsystem.security;

import com.datalabeling.datalabelingsupportsystem.pojo.User;
import com.datalabeling.datalabelingsupportsystem.repository.Users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

//    @Override
//    public UserDetails loadUserByUsername(String username)
//            throws UsernameNotFoundException {
//
//        return userRepository.findByUsername(username)
//                .orElseThrow(() ->
//                        new UsernameNotFoundException("User not found: " + username)
//                );
//    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Not found"));

        System.out.println("LOGIN USER: " + user.getUsername());
        System.out.println("STATUS: " + user.getStatus());
        System.out.println("ROLE: " + user.getRole().getRoleName());

        System.out.println("AUTH -> " + user.getUsername()
                + " | role=" + user.getRole().getRoleName());

        return user;
    }

}
