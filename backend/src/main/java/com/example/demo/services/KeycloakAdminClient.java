package com.example.demo.services;

import com.example.demo.config.IdentidadNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Minimal Keycloak Admin REST client. It authenticates as the confidential service account
 * {@code logistica-admin} (client_credentials) and only does what account management needs: create, update,
 * change the app role of and delete users.
 */
@Component
public class KeycloakAdminClient {

    /** Thrown when Keycloak already has a user with that username or email. */
    public static class UsuarioExistenteException extends RuntimeException {}

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);

    private final RestClient http;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public KeycloakAdminClient(@Value("${app.keycloak.admin.base-url}") String baseUrl,
                               @Value("${app.keycloak.admin.realm}") String realm,
                               @Value("${app.keycloak.admin.client-id}") String clientId,
                               @Value("${app.keycloak.admin.client-secret}") String clientSecret) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /** Creates an enabled, verified user with a permanent password and the realm role {@code rol}; returns its id. */
    public String crearUsuario(String nombre, String email, String password, String rol) {
        return crearUsuario(nombre, email, password, rol, false);
    }

    /**
     * Creates an enabled, verified user with the realm role {@code rol}; returns its id. A {@code temporal} password
     * makes Keycloak force a change at the first web login.
     */
    public String crearUsuario(String nombre, String email, String password, String rol, boolean temporal) {
        try {
            String token = tokenAdmin();
            String id = crear(token, nombre, email, password, temporal);
            try {
                asignarRol(token, id, rol);
            } catch (RuntimeException e) {
                eliminarSilencioso(token, id);
                throw e;
            }
            return id;
        } catch (RestClientException e) {
            throw new IdentidadNoDisponibleException("Keycloak admin call failed: " + e.getClass().getSimpleName(), e);
        }
    }

    /**
     * Updates username/email/first and last name. Returns false when Keycloak has no such user (a profile linked to an
     * account that does not exist is only a local record). Throws {@link UsuarioExistenteException} on a duplicate.
     */
    public boolean actualizarUsuario(String id, String nombre, String email) {
        try {
            String token = tokenAdmin();
            String[] partes = partesNombre(nombre);
            Map<String, Object> body = Map.of("username", email, "email", email,
                    "firstName", partes[0], "lastName", partes[1]);
            boolean[] encontrado = {true};
            http.put().uri("/admin/realms/{realm}/users/{id}", realm, id)
                    .headers(h -> h.setBearerAuth(token)).contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve()
                    .onStatus(s -> s.value() == 404, (req, r) -> encontrado[0] = false)
                    .onStatus(s -> s.value() == 409, (req, r) -> { throw new UsuarioExistenteException(); })
                    .onStatus(HttpStatusCode::isError, (req, r) -> {
                        throw new IdentidadNoDisponibleException("Keycloak update user returned " + r.getStatusCode());
                    })
                    .toBodilessEntity();
            return encontrado[0];
        } catch (RestClientException e) {
            throw new IdentidadNoDisponibleException("Keycloak admin call failed: " + e.getClass().getSimpleName(), e);
        }
    }

    /** Swaps the app realm role of a user ({@code anterior} out, {@code nuevo} in). False when the user is not in Keycloak. */
    public boolean cambiarRol(String id, String anterior, String nuevo) {
        try {
            String token = tokenAdmin();
            Map<?, ?> actual = http.get().uri("/admin/realms/{realm}/users/{id}", realm, id)
                    .headers(h -> h.setBearerAuth(token)).retrieve()
                    .onStatus(s -> s.value() == 404, (req, r) -> { })
                    .body(Map.class);
            if (actual == null || actual.get("id") == null) {
                return false;
            }
            Map<?, ?> repNuevo = representacionRol(token, nuevo);
            Map<?, ?> repAnterior = representacionRol(token, anterior);
            http.post().uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, id)
                    .headers(h -> h.setBearerAuth(token)).contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(repNuevo)).retrieve().toBodilessEntity();
            http.method(HttpMethod.DELETE).uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, id)
                    .headers(h -> h.setBearerAuth(token)).contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(repAnterior)).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            throw new IdentidadNoDisponibleException("Keycloak admin call failed: " + e.getClass().getSimpleName(), e);
        }
    }

    /** Removes a user; never throws. Returns true when it is gone (including "was already absent"). */
    public boolean eliminarUsuario(String id) {
        try {
            return eliminarSilencioso(tokenAdmin(), id);
        } catch (RuntimeException e) {
            log.warn("Keycloak delete of user {} failed: {}", id, e.getClass().getSimpleName());
            return false;
        }
    }

    private String tokenAdmin() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        Map<?, ?> res = http.post().uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                .retrieve().body(Map.class);
        Object token = res == null ? null : res.get("access_token");
        if (token == null) {
            throw new IdentidadNoDisponibleException("Keycloak admin token missing");
        }
        return token.toString();
    }

    /** Keycloak's default user profile refuses to log in an account without first and last name. */
    private static String[] partesNombre(String nombre) {
        String[] partes = nombre.trim().split("\\s+", 2);
        return new String[]{partes[0], partes.length > 1 ? partes[1] : "-"};
    }

    private String crear(String token, String nombre, String email, String password, boolean temporal) {
        String[] partes = partesNombre(nombre);
        Map<String, Object> body = Map.of(
                "username", email,
                "email", email,
                "firstName", partes[0],
                "lastName", partes[1],
                "enabled", true,
                "emailVerified", true,
                "credentials", List.of(Map.of("type", "password", "value", password, "temporary", temporal)));
        var res = http.post().uri("/admin/realms/{realm}/users", realm)
                .headers(h -> h.setBearerAuth(token)).contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve()
                .onStatus(s -> s.value() == 409, (req, r) -> { throw new UsuarioExistenteException(); })
                .onStatus(HttpStatusCode::isError, (req, r) -> {
                    throw new IdentidadNoDisponibleException("Keycloak create user returned " + r.getStatusCode());
                })
                .toBodilessEntity();
        URI location = res.getHeaders().getLocation();
        if (location == null) {
            throw new IdentidadNoDisponibleException("Keycloak create user without Location");
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private Map<?, ?> representacionRol(String token, String rol) {
        return http.get().uri("/admin/realms/{realm}/roles/{rol}", realm, rol)
                .headers(h -> h.setBearerAuth(token)).retrieve().body(Map.class);
    }

    private void asignarRol(String token, String id, String rol) {
        Map<?, ?> representacion = representacionRol(token, rol);
        http.post().uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, id)
                .headers(h -> h.setBearerAuth(token)).contentType(MediaType.APPLICATION_JSON)
                .body(List.of(representacion)).retrieve().toBodilessEntity();
    }

    private boolean eliminarSilencioso(String token, String id) {
        try {
            http.delete().uri("/admin/realms/{realm}/users/{id}", realm, id)
                    .headers(h -> h.setBearerAuth(token)).retrieve()
                    .onStatus(s -> s.value() == 404, (req, r) -> { })
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            return false; // best effort
        }
    }
}
