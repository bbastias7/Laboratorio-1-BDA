package com.example.demo.services;

import com.example.demo.dto.Dtos.UsuarioCrearRequest;
import com.example.demo.entities.UsuarioEntity;
import com.example.demo.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {

    private final KeycloakAdminClient keycloak = mock(KeycloakAdminClient.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final UsuarioService service = new UsuarioService(usuarios, keycloak);

    private static UsuarioCrearRequest conPassword() {
        return new UsuarioCrearRequest("Luis Soto", " Luis@Ejemplo.cl ", "repartidor", "claveTemporal1", null);
    }

    private static UsuarioEntity existente() {
        return new UsuarioEntity(7, "Luis Soto", "luis@ejemplo.cl", "repartidor", "kc-7");
    }

    @Test
    void createsKeycloakAccountWithTemporaryPasswordThenProfile() {
        when(usuarios.findByEmail("luis@ejemplo.cl")).thenReturn(Optional.empty());
        when(keycloak.crearUsuario("Luis Soto", "luis@ejemplo.cl", "claveTemporal1", "repartidor", true)).thenReturn("kc-1");
        when(usuarios.save(any())).thenAnswer(i -> i.getArgument(0));

        UsuarioEntity creado = service.saveUsuario(conPassword());

        assertEquals("kc-1", creado.getKeycloakId());
        assertEquals("luis@ejemplo.cl", creado.getEmail());
        verify(keycloak, never()).eliminarUsuario(any());
    }

    @Test
    void linksExistingAccountWithoutTouchingKeycloak() {
        when(usuarios.findByEmail(any())).thenReturn(Optional.empty());
        when(usuarios.save(any())).thenAnswer(i -> i.getArgument(0));

        UsuarioEntity creado = service.saveUsuario(new UsuarioCrearRequest("Ana", "ana@ejemplo.cl", "cliente", null, "kc-9"));

        assertEquals("kc-9", creado.getKeycloakId());
        verifyNoInteractions(keycloak);
    }

    @Test
    void requiresExactlyOneOfPasswordOrKeycloakId() {
        assertThrows(IllegalArgumentException.class,
                () -> service.saveUsuario(new UsuarioCrearRequest("Ana", "a@ejemplo.cl", "cliente", "claveTemporal1", "kc-9")));
        assertThrows(IllegalArgumentException.class,
                () -> service.saveUsuario(new UsuarioCrearRequest("Ana", "a@ejemplo.cl", "cliente", null, null)));
        verifyNoInteractions(keycloak);
    }

    @Test
    void rejectsShortPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> service.saveUsuario(new UsuarioCrearRequest("Ana", "a@ejemplo.cl", "cliente", "corta", null)));
        verifyNoInteractions(keycloak);
    }

    @Test
    void rejectsDuplicateEmailInDatabaseAndInKeycloak() {
        when(usuarios.findByEmail("luis@ejemplo.cl")).thenReturn(Optional.of(existente()));
        assertThrows(IllegalStateException.class, () -> service.saveUsuario(conPassword()));
        verifyNoInteractions(keycloak);

        when(usuarios.findByEmail("luis@ejemplo.cl")).thenReturn(Optional.empty());
        when(keycloak.crearUsuario(any(), any(), any(), any(), anyBoolean()))
                .thenThrow(new KeycloakAdminClient.UsuarioExistenteException());
        assertThrows(IllegalStateException.class, () -> service.saveUsuario(conPassword()));
        verify(usuarios, never()).save(any());
    }

    @Test
    void deletesKeycloakUserWhenProfileInsertFails() {
        when(usuarios.findByEmail(any())).thenReturn(Optional.empty());
        when(keycloak.crearUsuario(any(), any(), any(), any(), anyBoolean())).thenReturn("kc-2");
        when(usuarios.save(any())).thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class, () -> service.saveUsuario(conPassword()));
        verify(keycloak).eliminarUsuario("kc-2");
    }

    @Test
    void roleChangeUpdatesKeycloakMappingBeforeDatabase() {
        when(usuarios.findById(7)).thenReturn(Optional.of(existente()));
        when(keycloak.cambiarRol("kc-7", "repartidor", "despachador")).thenReturn(true);
        when(usuarios.update(any())).thenReturn(1);

        UsuarioEntity res = service.updateUsuario(7,
                new UsuarioEntity(7, "Luis Soto", "luis@ejemplo.cl", "despachador", null), 1);

        assertEquals("despachador", res.getRol());
        var orden = inOrder(keycloak, usuarios);
        orden.verify(keycloak).cambiarRol("kc-7", "repartidor", "despachador");
        orden.verify(usuarios).update(any());
        verify(keycloak, never()).actualizarUsuario(any(), any(), any());
    }

    @Test
    void nameOrEmailChangeUpdatesKeycloakUser() {
        when(usuarios.findById(7)).thenReturn(Optional.of(existente()));
        when(usuarios.findByEmail("nuevo@ejemplo.cl")).thenReturn(Optional.empty());
        when(keycloak.actualizarUsuario("kc-7", "Luis S.", "nuevo@ejemplo.cl")).thenReturn(true);
        when(usuarios.update(any())).thenReturn(1);

        service.updateUsuario(7, new UsuarioEntity(7, "Luis S.", "nuevo@ejemplo.cl", "repartidor", null), 1);

        verify(keycloak).actualizarUsuario("kc-7", "Luis S.", "nuevo@ejemplo.cl");
        verify(keycloak, never()).cambiarRol(any(), any(), any());
    }

    @Test
    void rollsKeycloakBackWhenDatabaseUpdateFails() {
        when(usuarios.findById(7)).thenReturn(Optional.of(existente()));
        when(keycloak.cambiarRol("kc-7", "repartidor", "despachador")).thenReturn(true);
        when(usuarios.update(any())).thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class, () -> service.updateUsuario(7,
                new UsuarioEntity(7, "Luis Soto", "luis@ejemplo.cl", "despachador", null), 1));

        verify(keycloak).cambiarRol("kc-7", "despachador", "repartidor");
    }

    @Test
    void doesNotChangeKeycloakWhenEmailTakenByAnotherUser() {
        when(usuarios.findById(7)).thenReturn(Optional.of(existente()));
        when(usuarios.findByEmail("otro@ejemplo.cl"))
                .thenReturn(Optional.of(new UsuarioEntity(8, "Otro", "otro@ejemplo.cl", "cliente", "kc-8")));

        assertThrows(IllegalStateException.class, () -> service.updateUsuario(7,
                new UsuarioEntity(7, "Luis Soto", "otro@ejemplo.cl", "repartidor", null), 1));
        verifyNoInteractions(keycloak);
    }

    @Test
    void keycloakIdStaysImmutable() {
        when(usuarios.findById(7)).thenReturn(Optional.of(existente()));
        assertThrows(IllegalArgumentException.class, () -> service.updateUsuario(7,
                new UsuarioEntity(7, "Luis Soto", "luis@ejemplo.cl", "repartidor", "otro"), 1));
    }

    @Test
    void dispatcherCannotChangeOwnRoleOrDeleteSelf() {
        when(usuarios.findById(7)).thenReturn(Optional.of(
                new UsuarioEntity(7, "Admin", "admin@ejemplo.cl", "despachador", "kc-7")));

        assertThrows(IllegalStateException.class, () -> service.updateUsuario(7,
                new UsuarioEntity(7, "Admin", "admin@ejemplo.cl", "cliente", null), 7));
        assertThrows(IllegalStateException.class, () -> service.deleteById(7, 7));
        verify(usuarios, never()).deleteById(any());
        verifyNoInteractions(keycloak);
    }

    @Test
    void deleteRemovesKeycloakUserAfterDatabaseRow() {
        when(usuarios.findById(7)).thenReturn(Optional.of(existente()));
        when(usuarios.deleteById(7)).thenReturn(1);
        when(keycloak.eliminarUsuario("kc-7")).thenReturn(true);

        service.deleteById(7, 1);

        var orden = inOrder(usuarios, keycloak);
        orden.verify(usuarios).deleteById(7);
        orden.verify(keycloak).eliminarUsuario("kc-7");
    }

    @Test
    void deleteSucceedsEvenIfKeycloakDeleteFails() {
        when(usuarios.findById(7)).thenReturn(Optional.of(existente()));
        when(usuarios.deleteById(7)).thenReturn(1);
        when(keycloak.eliminarUsuario("kc-7")).thenReturn(false);

        assertDoesNotThrow(() -> service.deleteById(7, 1));
    }

    @Test
    void keepsKeycloakWhenDatabaseDeleteIsRejected() {
        when(usuarios.findById(7)).thenReturn(Optional.of(existente()));
        when(usuarios.deleteById(7)).thenThrow(new org.springframework.dao.DataIntegrityViolationException("fk"));

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> service.deleteById(7, 1));
        verify(keycloak, never()).eliminarUsuario(any());
    }
}
