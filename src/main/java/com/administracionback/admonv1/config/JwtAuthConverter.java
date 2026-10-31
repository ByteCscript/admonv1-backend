package com.administracionback.admonv1.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/*
 * Convierte el JWT propio (claim `roles` generado por JwtService) en
 * autoridades Spring Security.
 *
 * Sin este converter, el converter por defecto de oauth2ResourceServer solo
 * lee el claim `scope`, de modo que `hasRole("ADMIN")` denegaría hasta al
 * administrador. Los valores del claim ya incluyen el prefijo `ROLE_`
 * (ver JwtService.generateToken), por eso se usan tal cual.
 */
@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String ROLES_CLAIM = "roles";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        List<String> roles = jwt.getClaimAsStringList(ROLES_CLAIM);

        Collection<GrantedAuthority> authorities = roles == null
                ? List.of()
                : roles.stream()
                        .filter(role -> role != null && role.startsWith("ROLE_"))
                        .map(SimpleGrantedAuthority::new)
                        .map(GrantedAuthority.class::cast)
                        .toList();

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}
