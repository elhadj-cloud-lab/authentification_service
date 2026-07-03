package com.bestech.authentification_service.init;

import com.bestech.authentification_service.model.MyUser;
import com.bestech.authentification_service.model.Role;
import com.bestech.authentification_service.repository.RoleRepository;
import com.bestech.authentification_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initializeRoles();
        initializeDefaultAdmin();
    }

    private void initializeRoles() {
        createRoleIfAbsent("USER");
        createRoleIfAbsent("ADMIN");
    }

    private void createRoleIfAbsent(String roleName) {
        if (!roleRepository.existsByRole(roleName)) {
            roleRepository.save(Role.builder().role(roleName).build());
            log.info("Rôle '{}' initialisé", roleName);
        }
    }

    private void initializeDefaultAdmin() {
        Role adminRole = roleRepository.findByRole("ADMIN");

        if (adminRole != null && userRepository.findByEmail("admin@example.com").isEmpty()) {
            MyUser admin = new MyUser();
            admin.setUsername("admin");
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("admin1234"));
            admin.setEnabled(true);
            admin.setRoles(List.of(adminRole));

            userRepository.save(admin);
            log.info("Compte admin par défaut créé — email: admin@example.com");
        }
    }
}
