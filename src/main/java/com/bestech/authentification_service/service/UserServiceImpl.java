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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Transactional
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    VerificationTokenRepository verificationTokenRepository;

    @Autowired
    EmailSender emailSender;

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
        Optional<MyUser>  optionalUser = userRepository.findByEmail(request.getEmail());
        if(optionalUser.isPresent())
            throw new EmailAlreadyExistsException("Email déjà existant!");

        MyUser newUser = new MyUser();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());

        newUser.setPassword( bCryptPasswordEncoder.encode( request.getPassword() )  );
        newUser.setEnabled(false);

        userRepository.save(newUser);

        Role r = roleRepository.findByRole("USER");
        List<Role> roles = new ArrayList<>();
        roles.add(r);
        newUser.setRoles(roles);

        //génére le code secret
        String code = this.generateCode();

        VerificationToken token = new VerificationToken(code, newUser);
        verificationTokenRepository.save(token);

        //envoyer le code par email à l'utilisateur
        sendEmailUser(newUser,token.getToken());


        return userRepository.save(newUser);
    }

    private String generateCode() {
        Random random = new Random();
        Integer code = 100000 + random.nextInt(900000);

        return code.toString();

    }

    @Override
    public void sendEmailUser(MyUser user, String code) {
        String emailBody ="Bonjour "+ "<h1>"+user.getUsername() +"</h1>" +
                " Votre code de validation est "+"<h1>"+code+"</h1>";

        emailSender.sendEmail(user.getEmail(), emailBody);
    }

    @Override
    public MyUser validateToken(String code) {
        VerificationToken token = verificationTokenRepository.findByToken(code);

        if(token == null){
            throw new InvalidTokenException("Invalid Token !!!!!!!");
        }

        MyUser user = token.getUser();

        Calendar calendar = Calendar.getInstance();

        if ((token.getExpirationTime().getTime() - calendar.getTime().getTime()) <= 0){
            verificationTokenRepository.delete(token);
            throw new ExpiredTokenException("expired Token");
        }

        user.setEnabled(true);
        userRepository.save(user);
        return user;

    }
}
