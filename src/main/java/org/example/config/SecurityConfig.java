package org.example.config;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.example.repository.UserRepository;

import java.util.Locale;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return username -> {
            var user =
                    users.findByUsername(username.trim().toLowerCase(Locale.ROOT))
                            .orElseThrow(
                                    () ->
                                            new UsernameNotFoundException(
                                                    "Niepoprawny login lub hasło."));
            return User.withUsername(
                            user.getUsername())
                    .password(user.getPassword())
                    .roles(user.getRole().name())
                    .disabled(!user.isEnabled())
                    .build();
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(
                        rules ->
                                rules.requestMatchers("/login", "/css/**", "/error")
                                        .permitAll()
                                        .requestMatchers("/users/**")
                                        .hasRole("ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .formLogin(
                        form ->
                                form.loginPage("/login")
                                        .defaultSuccessUrl("/dashboard", true)
                                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .build();
    }
}
