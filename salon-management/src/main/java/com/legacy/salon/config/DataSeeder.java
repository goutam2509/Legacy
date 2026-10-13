package com.legacy.salon.config;

import com.legacy.salon.model.AppUser;
import com.legacy.salon.model.SalonService;
import com.legacy.salon.repo.ServiceRepository;
import com.legacy.salon.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(UserRepository users,
                           ServiceRepository services,
                           PasswordEncoder encoder,
                           @Value("${app.admin.username}") String adminUser,
                           @Value("${app.admin.password}") String adminPass) {
        return args -> {
            // Admin user: .env ke ADMIN_USERNAME / ADMIN_PASSWORD se
            AppUser admin = users.findByUsername(adminUser).orElseGet(() -> {
                AppUser u = new AppUser();
                u.username = adminUser;
                return u;
            });
            // Har start par password .env ke hisaab se sync hota hai
            admin.passwordHash = encoder.encode(adminPass);
            users.save(admin);

            // Starter services (sirf pehli baar, jab table khali ho)
            if (services.count() == 0) {
                add(services, "Haircut", "250", 30);
                add(services, "Shave / Beard Trim", "100", 15);
                add(services, "Facial", "600", 45);
                add(services, "Hair Color", "1200", 90);
                add(services, "Head Massage", "200", 20);
            }
        };
    }

    private void add(ServiceRepository repo, String name, String price, int minutes) {
        SalonService s = new SalonService();
        s.name = name;
        s.price = new BigDecimal(price);
        s.durationMinutes = minutes;
        repo.save(s);
    }
}
