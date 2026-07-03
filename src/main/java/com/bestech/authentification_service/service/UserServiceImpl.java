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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Transactional
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailSender emailSender;

    @Override
    public MyUser saveUser(MyUser user) {
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public MyUser findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Role addRole(Role role) {
        return roleRepository.save(role);
    }

    @Override
    public MyUser addRoleToUser(String username, String rolename) {
        MyUser user = findUserByUsername(username);
        Role role = roleRepository.findByRole(rolename);
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    @Override
    public List<MyUser> findAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public MyUser registerUser(RegistrationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email déjà existant!");
        }

        MyUser newUser = new MyUser();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(bCryptPasswordEncoder.encode(request.getPassword()));
        newUser.setEnabled(false);

        Role r = roleRepository.findByRole("USER");
        newUser.setRoles(new ArrayList<>(List.of(r)));

        MyUser savedUser = userRepository.save(newUser);

        String code = generateCode();
        verificationTokenRepository.save(new VerificationToken(code, newUser));
        sendEmailUser(newUser, code);

        return savedUser;
    }

    private String generateCode() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    @Override
    public void sendEmailUser(MyUser user, String code) {
        String emailBody = "Bonjour " + "<h1>" + user.getUsername() + "</h1>"
                + " Votre code de validation est " + "<h1>" + code + "</h1>";
        emailSender.sendEmail(user.getEmail(), emailBody);
    }

    @Override
    public MyUser validateToken(String code) {
        VerificationToken token = verificationTokenRepository.findByToken(code);

        if (token == null) {
            throw new InvalidTokenException("Token invalide");
        }

        Calendar calendar = Calendar.getInstance();
        if ((token.getExpirationTime().getTime() - calendar.getTime().getTime()) <= 0) {
            verificationTokenRepository.delete(token);
            throw new ExpiredTokenException("Token expiré");
        }

        MyUser user = token.getUser();
        user.setEnabled(true);
        return userRepository.save(user);
    }
}
