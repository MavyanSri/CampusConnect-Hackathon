package com.mediflow.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mediflow.backend.model.User;
import com.mediflow.backend.model.UserRole;
import com.mediflow.backend.repository.UserRepository;

@Configuration
@ConditionalOnProperty(name = "mediflow.demo.admin.password")
public class LocalDemoAdminConfig {

    private static final String ADMIN_EMAIL = "admin.demo@example.com";

    @Bean
    public CommandLineRunner createDemoAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${mediflow.demo.admin.password}") String password) {

        return args -> {
            var existingUser = userRepository.findByEmail(ADMIN_EMAIL);

            if (existingUser.isPresent()) {
                if (existingUser.get().getRole() != UserRole.ADMIN) {
                    throw new IllegalStateException(
                            "Demo admin email already belongs to another account");
                }
                return;
            }

            if (password.isBlank() || password.length() < 12) {
                throw new IllegalStateException(
                        "Set a demo admin password of at least 12 characters");
            }

            User admin = new User();
            admin.setFullName("MediFlow Demo Admin");
            admin.setEmail(ADMIN_EMAIL);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setRole(UserRole.ADMIN);
            admin.setActive(true);

            userRepository.save(admin);
        };
    }
}