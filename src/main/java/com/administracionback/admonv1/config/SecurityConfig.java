package com.administracionback.admonv1.config;

import com.administracionback.admonv1.dto.ApiResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthConverter jwtAuthConverter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/health").permitAll()

                        // Swagger público
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // H2 Console pública
                        .requestMatchers("/h2-console/**").permitAll()

                        // RN-06: solo ADMIN escribe convocatorias (antes de anyRequest).
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/calls",
                                "/api/calls/**"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/calls",
                                "/api/calls/**"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/calls",
                                "/api/calls/**"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/calls",
                                "/api/calls/**"
                        ).hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .headers(headers ->
                                headers
                                        .frameOptions(frame -> frame.disable())
                        // temporalmente para desarrollo
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(jwtAuthConverter)
                        )
                )
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                );


        return http.build();
    }

    /*
     * 401 con ApiResponse estandarizado cuando no hay (o es inválido) el JWT.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (HttpServletRequest request,
                HttpServletResponse response,
                AuthenticationException authException) -> writeError(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Se requiere autenticación para acceder al recurso",
                        "UNAUTHORIZED"
                );
    }

    /*
     * 403 con ApiResponse estandarizado cuando el rol no alcanza (RN-06).
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest request,
                HttpServletResponse response,
                org.springframework.security.access.AccessDeniedException accessDeniedException) ->
                writeError(
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        "No tiene permisos para ejecutar esta operación",
                        "FORBIDDEN"
                );
    }

    private void writeError(
            HttpServletResponse response,
            int status,
            String message,
            String code) throws java.io.IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(
                        new ApiResponse<>(message, null, code)
                )
        );
    }
}
