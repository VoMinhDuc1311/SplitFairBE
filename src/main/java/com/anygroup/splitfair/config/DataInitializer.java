package com.anygroup.splitfair.config;

import com.anygroup.splitfair.enums.RoleType;
import com.anygroup.splitfair.model.Role;
import com.anygroup.splitfair.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
         for (RoleType type : RoleType.values()) {
             if (!roleRepository.existsByName(type)) {
                 roleRepository.save(Role.builder().name(type).build());
                 System.out.println(" Created role: " + type);
             }
         }
    }
}
