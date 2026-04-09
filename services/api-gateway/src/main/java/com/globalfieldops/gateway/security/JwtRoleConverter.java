package com.globalfieldops.gateway.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Extracts roles from the JWT "roles" claim and maps them to Spring Security ROLE_* authorities.
 * Merges with the default scope-based authorities from JwtGrantedAuthoritiesConverter.
 */
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection<GrantedAuthority> defaultAuthorities = defaultConverter.convert(jwt);

        List<String> roles = jwt.getClaimAsStringList(ROLES_CLAIM);
        if (roles == null || roles.isEmpty()) {
            return defaultAuthorities;
        }

        List<GrantedAuthority> roleAuthorities = roles.stream()
                .map(role -> role.toUpperCase().startsWith(ROLE_PREFIX) ? role.toUpperCase() : ROLE_PREFIX + role.toUpperCase())
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();

        return Stream.concat(
                defaultAuthorities != null ? defaultAuthorities.stream() : Stream.empty(),
                roleAuthorities.stream()
        ).toList();
    }
}
