package lk.campusslot.config;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import lk.campusslot.auth.*;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean UserDetailsService userDetailsService(UserRepository users) {
        return input -> {
            String email;
            try { email = CampusEmail.normalize(input); }
            catch (RuntimeException ex) { throw new UsernameNotFoundException("Invalid credentials"); }
            var user = users.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
            return User.withUsername(user.getEmail()).password(user.getPasswordHash())
                    .roles(user.getRole().name()).disabled(!user.isVerified()).build();
        };
    }
    @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Session CSRF token is fetched explicitly by the SPA; the default BREACH protection stays enabled.
        http.authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/verify", "/api/auth/login").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/bookings").hasRole("STUDENT")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll())
            .formLogin(f -> f.loginProcessingUrl("/api/auth/login")
                .successHandler((req,res,a) -> res.setStatus(204))
                .failureHandler((req,res,e) -> { res.setStatus(401); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Check your email, password and account verification.\"}"); }))
            .logout(l -> l.logoutUrl("/api/auth/logout").invalidateHttpSession(true).deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req,res,a) -> res.setStatus(204)))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req,res,x) -> { res.setStatus(401); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Please log in.\"}"); })
                .accessDeniedHandler((req,res,x) -> { res.setStatus(403); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Request denied. Refresh the page or check your permissions.\"}"); }));
        return http.build();
    }
}
