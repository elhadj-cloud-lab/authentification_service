package com.bestech.authentification_service.service;

import com.bestech.authentification_service.model.MyUser;
import com.bestech.authentification_service.model.Role;
import com.bestech.authentification_service.repository.RoleRepository;
import com.bestech.authentification_service.repository.UserRepository;
import com.bestech.authentification_service.service.exceptions.EmailAlreadyExistsException;
import com.bestech.authentification_service.service.exceptions.ExpiredTokenException;
import com.bestech.authentification_service.service.exceptions.InvalidTokenException;
import com.bestech.authentification_service.service.register.RegistrationRequest;
import com.bestech.authentification_service.service.register.VerificationToken;
import com.bestech.authentification_service.service.register.VerificationTokenRepository;
import com.bestech.authentification_service.util.EmailSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock BCryptPasswordEncoder bCryptPasswordEncoder;
    @Mock VerificationTokenRepository verificationTokenRepository;
    @Mock EmailSender emailSender;

    @InjectMocks UserServiceImpl userService;

    @Test
    void saveUser_encodesPasswordBeforeSaving() {
        MyUser user = new MyUser();
        user.setPassword("plaintext");
        when(bCryptPasswordEncoder.encode("plaintext")).thenReturn("$2a$hashed");
        when(userRepository.save(user)).thenReturn(user);

        userService.saveUser(user);

        assertThat(user.getPassword()).isEqualTo("$2a$hashed");
        verify(userRepository).save(user);
    }

    @Test
    void findUserByUsername_delegatesToRepository() {
        MyUser expected = new MyUser();
        when(userRepository.findByUsername("alice")).thenReturn(expected);

        MyUser result = userService.findUserByUsername("alice");

        assertThat(result).isSameAs(expected);
    }

    @Test
    void findAllUsers_returnsRepositoryResult() {
        when(userRepository.findAll()).thenReturn(List.of(new MyUser(), new MyUser()));

        List<MyUser> users = userService.findAllUsers();

        assertThat(users).hasSize(2);
    }

    @Test
    void addRole_savesToRepository() {
        Role role = new Role();
        when(roleRepository.save(role)).thenReturn(role);

        Role result = userService.addRole(role);

        assertThat(result).isSameAs(role);
    }

    @Test
    void addRoleToUser_addsRoleAndSavesUser() {
        MyUser user = new MyUser();
        user.setRoles(new ArrayList<>());
        Role role = new Role();
        role.setRole("ADMIN");

        when(userRepository.findByUsername("alice")).thenReturn(user);
        when(roleRepository.findByRole("ADMIN")).thenReturn(role);
        when(userRepository.save(user)).thenReturn(user);

        userService.addRoleToUser("alice", "ADMIN");

        assertThat(user.getRoles()).contains(role);
    }

    @Test
    void registerUser_savesUserAndSendsVerificationEmail() {
        RegistrationRequest request = new RegistrationRequest("alice", "pass123", "alice@test.com");
        Role userRole = new Role();
        userRole.setRole("USER");

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.empty());
        when(bCryptPasswordEncoder.encode("pass123")).thenReturn("hashed");
        when(roleRepository.findByRole("USER")).thenReturn(userRole);
        when(userRepository.save(any(MyUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(verificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MyUser result = userService.registerUser(request);

        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getEmail()).isEqualTo("alice@test.com");
        assertThat(result.getEnabled()).isFalse();
        verify(emailSender).sendEmail(eq("alice@test.com"), any(String.class));
        verify(verificationTokenRepository).save(any(VerificationToken.class));
    }

    @Test
    void registerUser_throwsEmailAlreadyExistsException_whenEmailDuplicate() {
        when(userRepository.findByEmail("dup@test.com")).thenReturn(Optional.of(new MyUser()));

        assertThatThrownBy(() ->
                userService.registerUser(new RegistrationRequest("bob", "pass", "dup@test.com")))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void validateToken_enablesUserWhenTokenIsValid() {
        MyUser user = new MyUser();
        user.setEnabled(false);

        VerificationToken token = new VerificationToken("123456", user);
        // Token expiration is 15 min in the future by default constructor

        when(verificationTokenRepository.findByToken("123456")).thenReturn(token);
        when(userRepository.save(user)).thenReturn(user);

        MyUser result = userService.validateToken("123456");

        assertThat(result.getEnabled()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void validateToken_throwsInvalidTokenException_whenTokenNotFound() {
        when(verificationTokenRepository.findByToken("bad")).thenReturn(null);

        assertThatThrownBy(() -> userService.validateToken("bad"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validateToken_throwsExpiredTokenException_andDeletesToken_whenExpired() {
        MyUser user = new MyUser();
        VerificationToken token = new VerificationToken("expired", user);
        token.setExpirationTime(new Date(System.currentTimeMillis() - 1000)); // past

        when(verificationTokenRepository.findByToken("expired")).thenReturn(token);

        assertThatThrownBy(() -> userService.validateToken("expired"))
                .isInstanceOf(ExpiredTokenException.class);

        verify(verificationTokenRepository).delete(token);
    }

    @Test
    void sendEmailUser_sendsEmailWithCodeInBody() {
        MyUser user = new MyUser();
        user.setUsername("carol");
        user.setEmail("carol@test.com");

        userService.sendEmailUser(user, "654321");

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendEmail(eq("carol@test.com"), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).contains("654321").contains("carol");
    }
}
