package com.administracionback.admonv1.auth;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.model.User;
import com.administracionback.admonv1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

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

        LoginResponseDTO response = new LoginResponseDTO(
                token,
                "Bearer",
                3600
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