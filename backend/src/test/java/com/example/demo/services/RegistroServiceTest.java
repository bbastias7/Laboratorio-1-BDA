package com.example.demo.services;

import com.example.demo.dto.Dtos.RegistroRequest;
import com.example.demo.entities.UsuarioEntity;
import com.example.demo.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegistroServiceTest {

    private final KeycloakAdminClient keycloak = mock(KeycloakAdminClient.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final RegistroService service = new RegistroService(keycloak, usuarios);

    private static RegistroRequest request() {
        return new RegistroRequest("Ana Pérez", "  Ana@Ejemplo.cl ", "claveSegura1");
    }

    @Test
    void createsClienteProfileWithNormalizedEmail() {
        when(usuarios.findByEmail("ana@ejemplo.cl")).thenReturn(Optional.empty());
        when(keycloak.crearUsuario("Ana Pérez", "ana@ejemplo.cl", "claveSegura1", "cliente")).thenReturn("kc-1");
        when(usuarios.save(any())).thenAnswer(i -> i.getArgument(0));

        UsuarioEntity creado = service.registrar(request());

        assertEquals("cliente", creado.getRol());
        assertEquals("ana@ejemplo.cl", creado.getEmail());
        assertEquals("kc-1", creado.getKeycloakId());
        verify(keycloak, never()).eliminarUsuario(any());
    }

    @Test
    void rejectsEmailAlreadyInDatabaseWithoutCallingKeycloak() {
        when(usuarios.findByEmail("ana@ejemplo.cl")).thenReturn(Optional.of(new UsuarioEntity()));

        assertThrows(IllegalStateException.class, () -> service.registrar(request()));
        verifyNoInteractions(keycloak);
    }

    @Test
    void mapsKeycloakDuplicateToConflict() {
        when(usuarios.findByEmail(any())).thenReturn(Optional.empty());
        when(keycloak.crearUsuario(any(), any(), any(), any()))
                .thenThrow(new KeycloakAdminClient.UsuarioExistenteException());

        assertThrows(IllegalStateException.class, () -> service.registrar(request()));
        verify(usuarios, never()).save(any());
    }

    @Test
    void deletesKeycloakUserWhenProfileInsertFails() {
        when(usuarios.findByEmail(any())).thenReturn(Optional.empty());
        when(keycloak.crearUsuario(any(), any(), any(), any())).thenReturn("kc-2");
        when(usuarios.save(any())).thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class, () -> service.registrar(request()));
        verify(keycloak).eliminarUsuario("kc-2");
    }
}
