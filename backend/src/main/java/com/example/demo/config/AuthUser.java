package com.example.demo.config;

import org.springframework.security.oauth2.jwt.Jwt;

/** Authenticated caller, taken from the JWT claims {@code uid} and {@code rol}. */
public record AuthUser(Integer uid, String rol) {

    public static AuthUser from(Jwt jwt) {
        Number uid = jwt.getClaim("uid");
        return new AuthUser(uid == null ? null : uid.intValue(), jwt.getClaimAsString("rol"));
    }

    public boolean esCliente() { return "cliente".equals(rol); }
    public boolean esRepartidor() { return "repartidor".equals(rol); }
    public boolean esDespachador() { return "despachador".equals(rol); }
}
