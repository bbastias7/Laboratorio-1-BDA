package com.example.demo.config;

import com.example.demo.repositories.UsuarioRepository;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.keycloak.issuer}") String issuer,
                          @Value("${app.keycloak.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(issuer + "/protocol/openid-connect/certs").build();
        OAuth2TokenValidator<Jwt> validarAudiencia = jwt -> jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Audiencia incorrecta", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), validarAudiencia));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioRepository usuarios) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(token -> autenticar(token, usuarios)))
                        .authenticationEntryPoint((request, response, error) -> escribirError(response, 401, "No autenticado"))
                        .accessDeniedHandler((request, response, error) -> escribirError(response, 403, "Acceso denegado")));
        return http.build();
    }

    // Vincular el sub validado de Keycloak al perfil SQL; no confiar en IDs del body.
    private JwtAuthenticationToken autenticar(Jwt token, UsuarioRepository usuarios) {
        if (token.getSubject() == null || token.getSubject().isBlank()) {
            throw new BadCredentialsException("JWT sin identidad");
        }
        var usuario = usuarios.findByKeycloakId(token.getSubject())
                .orElseThrow(() -> new BadCredentialsException("Identidad sin perfil local"));
        Map<String, Object> realm = token.getClaim("realm_access");
        Object roles = realm == null ? null : realm.get("roles");
        if (!(roles instanceof Collection<?> lista) || !lista.contains(usuario.getRol())) {
            throw new BadCredentialsException("Rol de Keycloak y perfil local no coinciden");
        }
        Map<String, Object> claims = new HashMap<>(token.getClaims());
        claims.put("uid", usuario.getUsuarioId());
        claims.put("rol", usuario.getRol());
        Jwt principal = new Jwt(token.getTokenValue(), token.getIssuedAt(), token.getExpiresAt(),
                token.getHeaders(), claims);
        return new JwtAuthenticationToken(principal,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().toUpperCase(Locale.ROOT))),
                token.getSubject());
    }

    @Bean
    CorsConfigurationSource cors(@Value("${app.cors.origins}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
    private static void escribirError(jakarta.servlet.http.HttpServletResponse response,
                                     int status, String mensaje) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + mensaje + "\"}");
    }
}
