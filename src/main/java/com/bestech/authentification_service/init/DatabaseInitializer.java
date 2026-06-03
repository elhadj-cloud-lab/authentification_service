package com.bestech.authentification_service.init;

import com.bestech.authentification_service.model.MyUser;
import com.bestech.authentification_service.model.Role;
import com.bestech.authentification_service.repository.RoleRepository;
import com.bestech.authentification_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
        initializeDefaultAdmin();
    }

    private void initializeRoles() {
        createRoleIfAbsent("USER");
        createRoleIfAbsent("ADMIN");
    }

    private void createRoleIfAbsent(String roleName) {
        if (!roleRepository.existsByRole(roleName)) {
            Role role = Role.builder()
                    .role(roleName)
                    .build();
            roleRepository.save(role);
            log.info("Rôle '{}' initialisé", roleName);
        }
    }

    // Optionnel : Créer un admin par défaut
    private void initializeDefaultAdmin() {
        Role adminRole = roleRepository.findByRole("ADMIN");

        if (adminRole != null && !userRepository.findByEmail("admin@example.com").isPresent()) {
            MyUser admin = new MyUser();
            admin.setUsername("admin");
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("admin1234"));
            admin.setEnabled(true);
            admin.setRoles(Arrays.asList(adminRole));

            userRepository.save(admin);
            log.info("Compte admin par défaut créé - email: admin@example.com, mot de passe: Admin123!");
        }
    }
}
