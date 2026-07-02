package com.bestech.authentification_service.security;

import com.bestech.authentification_service.model.MyUser;
import com.bestech.authentification_service.model.Role;
import com.bestech.authentification_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyUserDetailsServiceTest {

    @Mock
    UserService userService;

    @InjectMocks
    MyUserDetailsService myUserDetailsService;

    @Test
    void loadUserByUsername_returnsUserDetailsWithAuthorities() {
        Role adminRole = new Role();
        adminRole.setRole("ADMIN");

        MyUser user = new MyUser();
        user.setUsername("alice");
        user.setPassword("$2a$encoded");
        user.setEnabled(true);
        user.setRoles(List.of(adminRole));

        when(userService.findUserByUsername("alice")).thenReturn(user);

        UserDetails result = myUserDetailsService.loadUserByUsername("alice");

        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getPassword()).isEqualTo("$2a$encoded");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("ADMIN");
    }

    @Test
    void loadUserByUsername_withMultipleRoles_returnsAllAuthorities() {
        Role userRole = new Role();
        userRole.setRole("USER");
        Role adminRole = new Role();
        adminRole.setRole("ADMIN");

        MyUser user = new MyUser();
        user.setUsername("bob");
        user.setPassword("encoded");
        user.setEnabled(true);
        user.setRoles(List.of(userRole, adminRole));

        when(userService.findUserByUsername("bob")).thenReturn(user);

        UserDetails result = myUserDetailsService.loadUserByUsername("bob");

        assertThat(result.getAuthorities()).hasSize(2);
    }

    @Test
    void loadUserByUsername_withNoRoles_returnsEmptyAuthorities() {
        MyUser user = new MyUser();
        user.setUsername("carol");
        user.setPassword("encoded");
        user.setEnabled(false);
        user.setRoles(List.of());

        when(userService.findUserByUsername("carol")).thenReturn(user);

        UserDetails result = myUserDetailsService.loadUserByUsername("carol");

        assertThat(result.getAuthorities()).isEmpty();
        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_throwsUsernameNotFoundException_whenUserNotFound() {
        when(userService.findUserByUsername("unknown")).thenReturn(null);

        assertThatThrownBy(() -> myUserDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("introuvable");
    }
}
