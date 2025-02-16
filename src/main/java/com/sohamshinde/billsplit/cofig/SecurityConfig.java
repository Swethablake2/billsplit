package com.sohamshinde.billsplit.cofig;


import com.sohamshinde.billsplit.security.CustomAuthenticationEntryPoint;
import com.sohamshinde.billsplit.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(@Lazy UserService userService, CustomAuthenticationEntryPoint authenticationEntryPoint) {
        this.userService = userService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                .csrf(csrf -> csrf.disable()) // Disable CSRF protection (for testing only)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/user").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/user/verify").permitAll()
                        .requestMatchers("/api/lender/**").hasRole("LENDER") // Only lenders can access lender endpoints
                        .requestMatchers("/api/user/**", "/api/group/**", "/api/expenses/**").hasRole("USER") // Only users can access user endpoints
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/api/users/me", true)
                        .permitAll()
                )
                .httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(authenticationEntryPoint)) // Use custom entry point
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login"))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // Stateless session
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }


}