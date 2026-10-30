package com.administracionback.admonv1.auth;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.model.User;
import com.administracionback.admonv1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(
            LoginRequestDTO request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.email(),
                                request.password()
                        )
                );

        String token = jwtService.generateToken(authentication);

        // Datos de la sesión que el frontend usa para el guard por rol (D-i).
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        List<String> roles = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();

        LoginResponseDTO response = new LoginResponseDTO(
                token,
                "Bearer",
                3600,
                user.getEmail(),
                user.getName(),
                roles
        );


        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Login successful",
                        response,
                        null
                )
        );
    }
}
