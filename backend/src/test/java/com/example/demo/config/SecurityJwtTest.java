package com.example.demo.config;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.security.oauth2.jwt.*;
import static org.junit.jupiter.api.Assertions.*;

class SecurityJwtTest {
    private HttpServer server;
    private RSAKey key;
    private String issuer;
    private JwtDecoder decoder;

    @BeforeEach
    void setup() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("test-key").generate();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/realms/test/protocol/openid-connect/certs", exchange -> {
            byte[] bytes = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var stream = exchange.getResponseBody()) { stream.write(bytes); }
        });
        server.start();
        issuer = "http://127.0.0.1:" + server.getAddress().getPort() + "/realms/test";
        decoder = new SecurityConfig().jwtDecoder(issuer, "logistica-api");
    }

    @AfterEach void stop() { server.stop(0); }

    private String token(String iss, String aud, Instant expiration, RSAKey signingKey) {
        var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(signingKey)));
        var claims = JwtClaimsSet.builder().subject("usuario")
            .issuer(iss).audience(List.of(aud)).issuedAt(Instant.now().minusSeconds(7200))
            .expiresAt(expiration).build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @Test void acceptsExpectedIssuerAudienceAndSignature() {
        assertEquals("usuario", decoder.decode(token(issuer, "logistica-api", Instant.now().plusSeconds(60), key)).getSubject());
    }
    @Test void rejectsWrongIssuer() {
        assertThrows(JwtException.class, () -> decoder.decode(token("https://otro.example", "logistica-api", Instant.now().plusSeconds(60), key)));
    }
    @Test void rejectsWrongAudience() {
        assertThrows(JwtException.class, () -> decoder.decode(token(issuer, "otra-api", Instant.now().plusSeconds(60), key)));
    }
    @Test void rejectsExpiredToken() {
        assertThrows(JwtException.class, () -> decoder.decode(token(issuer, "logistica-api", Instant.now().minusSeconds(3600), key)));
    }
    @Test void rejectsWrongSignature() throws Exception {
        RSAKey other = new RSAKeyGenerator(2048).keyID("test-key").generate();
        assertThrows(JwtException.class, () -> decoder.decode(token(issuer, "logistica-api", Instant.now().plusSeconds(60), other)));
    }
}
