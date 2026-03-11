package com.datalabeling.datalabelingsupportsystem.config;

import com.datalabeling.datalabelingsupportsystem.pojo.Role;
import com.datalabeling.datalabelingsupportsystem.repository.Users.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        createRoleIfNotFound("ADMIN");
        createRoleIfNotFound("MANAGER");
        createRoleIfNotFound("ANNOTATOR");
        createRoleIfNotFound("REVIEWER");
        System.out.println(">>> Roles seeded successfully.");
    }

    private void createRoleIfNotFound(String roleName) {
        if (roleRepository.findByRoleName(roleName).isEmpty()) {
            Role role = new Role();
            role.setRoleName(roleName);
            roleRepository.save(role);
            System.out.println(">>> Created role: " + roleName);
        }
    }
}
