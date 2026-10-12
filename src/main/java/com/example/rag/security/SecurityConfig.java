package com.example.rag.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.io.IOException;
import java.util.Map;

/**
 * Session-based login for the single-page UI.
 * - Static page and /api/auth/** are public; everything else requires a signed-in user.
 * - Passwords are hashed with BCrypt.
 * - CSRF protection is on (cookie XSRF-TOKEN, header X-XSRF-TOKEN).
 * - Unauthenticated API calls get a plain 401 (no redirects).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper mapper = new ObjectMapper();

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/favicon.ico").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/")
                .loginProcessingUrl("/api/auth/login")
                .successHandler((req, res, auth) ->
                        writeJson(res, HttpServletResponse.SC_OK, Map.of("username", auth.getName())))
                .failureHandler((req, res, ex) -> {
                    if (ex instanceof LockedException) {
                        writeJson(res, 429, Map.of("error",
                                "Too many failed attempts. Try again in a few minutes."));
                    } else {
                        writeJson(res, HttpServletResponse.SC_UNAUTHORIZED,
                                Map.of("error", "Invalid username or password"));
                    }
                })
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_OK))
                .deleteCookies("JSESSIONID"))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; "
                + "img-src 'self' data:; frame-ancestors 'none'")));
        return http.build();
    }

    private void writeJson(HttpServletResponse res, int status, Object body) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getWriter(), body);
    }
}
