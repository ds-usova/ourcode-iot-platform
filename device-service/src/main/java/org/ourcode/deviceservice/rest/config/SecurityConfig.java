package org.ourcode.deviceservice.rest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.ourcode.deviceservice.rest.DeviceUtils.DEVICE_PATH;

@Configuration
public class SecurityConfig {

    private static final String CLIENT_ID = "device-service-app";

    private final boolean apiProtected;

    public SecurityConfig(@Value("${app.apiProtected}") boolean apiProtected) {
        this.apiProtected = apiProtected;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (apiProtected) {
            http.authorizeHttpRequests(authorize -> authorize
                            .requestMatchers(HttpMethod.GET, DEVICE_PATH + "/**").hasRole(Roles.DEVICE_READ.getRoleName())
                            .requestMatchers(HttpMethod.POST, DEVICE_PATH + "/**").hasRole(Roles.DEVICE_WRITE.getRoleName())
                            .requestMatchers(HttpMethod.PATCH, DEVICE_PATH + "/**").hasRole(Roles.DEVICE_WRITE.getRoleName())
                            .requestMatchers(HttpMethod.DELETE, DEVICE_PATH + "/**").hasRole(Roles.DEVICE_WRITE.getRoleName())
                            .anyRequest().authenticated()
                    )
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(jwt -> jwt
                                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                            )
                    );
        } else {
            http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        }

        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return jwtConverter;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // derive ROLE_* authorities from resource_access[CLIENT_ID].roles
        Object resourceAccess = asMap(jwt.getClaim("resource_access")).get(CLIENT_ID);

        return asStream(asMap(resourceAccess).get("roles"))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toSet());
    }

    private Map<?, ?> asMap(Object object) {
        return (object instanceof Map<?, ?> map) ? map : Collections.emptyMap();
    }

    private Stream<?> asStream(Object obj) {
        return (obj instanceof Collection<?> collection) ? collection.stream() : Stream.empty();
    }

}
