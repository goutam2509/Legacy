package com.legacy.salon.config;

import com.legacy.salon.repo.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository users) {
        return username -> users.findByUsername(username)
                .map(u -> User.withUsername(u.username)
                        .password(u.passwordHash)
                        .roles(u.role)
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User nahi mila"));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Simple internal tool: CSRF band. (Internet par public deploy karo to CSRF token add karna.)
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login.html", "/style.css", "/app.js", "/favicon.ico", "/favicon.png", "/logo.png").permitAll()
                // Meta (WhatsApp) webhook ko bina login ke access chahiye
                .requestMatchers("/webhook/**").permitAll()
                // Customer ka public booking page aur uski API (login nahi chahiye)
                .requestMatchers("/book.html", "/api/public/**").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .successHandler((req, res, auth) -> {
                    res.setStatus(HttpStatus.OK.value());
                    res.setContentType("application/json");
                    res.getWriter().write("{\"ok\":true}");
                })
                .failureHandler((req, res, ex) -> {
                    res.setStatus(HttpStatus.UNAUTHORIZED.value());
                    res.setContentType("application/json");
                    res.getWriter().write("{\"ok\":false,\"error\":\"Username ya password galat hai\"}");
                })
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpStatus.OK.value())))
            .exceptionHandling(ex -> ex
                // /api/** par login nahi hai to redirect ki jagah 401 do
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    new AntPathRequestMatcher("/api/**")))
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
