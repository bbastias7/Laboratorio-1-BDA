package com.example.demo.services;

import com.example.demo.entities.PaqueteEntity;
import com.example.demo.dto.Dtos.TrackingResponse;
import com.example.demo.dto.Dtos.EventoView;
import com.example.demo.entities.EventoSeguimientoEntity;
import com.example.demo.repositories.EventoSeguimientoRepository;
import com.example.demo.repositories.PaqueteRepository;
import com.example.demo.config.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class PaqueteService {
    private final PaqueteRepository paqueteRepository;
    private final EventoSeguimientoRepository eventoRepository;
    private final com.example.demo.repositories.RutaRepository rutaRepository;


    public List<PaqueteEntity> findAll() {
        return paqueteRepository.findAll();
    }


    public PaqueteEntity findById(Integer id) {
        return paqueteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Paquete inexistente"));
    }


    public PaqueteEntity findByCodigo(String codigo) {
        return paqueteRepository.findByCodigo(codigo)
                .orElseThrow(() -> new NoSuchElementException("Paquete inexistente"));
    }


    /** Role-scoped listing: the ownership filter is applied in SQL. */
    public List<PaqueteEntity> findVisibles(AuthUser u, String estado, Integer centroId) {
        if (estado != null && !java.util.Set.of("Registrado","En centro","En reparto","Entregado","Reprogramado","Devuelto a origen").contains(estado))
            throw new IllegalArgumentException("Estado no válido");
        return paqueteRepository.findVisibles(u.rol(), u.uid(), estado, centroId);
    }

    public PaqueteEntity findVisibleById(Integer id, AuthUser u) {
        PaqueteEntity p = findById(id);
        if (!paqueteRepository.esVisible(id, u.rol(), u.uid())) {
            throw new AccessDeniedException("Paquete de otro usuario");
        }
        return p;
    }

    /** Cliente registers for itself; despachador must name the cliente. The DB generates the tracking code. */
    @Transactional
    public PaqueteEntity registrar(PaqueteEntity p, AuthUser u) {
        if (u.esCliente()) {
            p.setUsuarioClienteId(u.uid());
        } else if (p.getUsuarioClienteId() == null) {
            throw new IllegalArgumentException("usuarioClienteId es obligatorio");
        }
        if (p.getCentroDistId() == null) {
            throw new IllegalArgumentException("centroDistId es obligatorio");
        }
        if (p.getDireccionDestino() == null || p.getDireccionDestino().isBlank()) {
            throw new IllegalArgumentException("direccionDestino es obligatoria");
        }
        // fields owned by the delivery lifecycle are never client-supplied
        p.setCodigo(null);
        p.setEstado("Registrado");
        p.setIntentos(0);
        p.setPaqueteOrigenId(null);
        p.setDespachadoAt(null);
        p.setEntregadoAt(null);
        p.setFechaReprogramada(null);
        if (p.getFechaCompromiso() == null) {
            throw new IllegalArgumentException("fechaCompromiso es obligatoria");
        }
        PaqueteEntity nuevo = paqueteRepository.save(p);
        eventoRepository.save(new EventoSeguimientoEntity(null, nuevo.getPaqueteId(), "Registrado",
                "Paquete registrado", LocalDateTime.now()));
        return nuevo;
    }

    /** Address change by the owner cliente or a despachador; the audit trigger reads app.user_id. */
    @Transactional
    public PaqueteEntity cambiarDireccion(Integer id, String direccion, AuthUser u) {
        PaqueteEntity p = findById(id);
        if (u.esCliente() && !paqueteRepository.esVisible(id, u.rol(), u.uid())) {
            throw new AccessDeniedException("Paquete de otro usuario");
        }
        if (!u.esCliente() && !u.esDespachador()) {
            throw new AccessDeniedException("Rol no autorizado");
        }
        paqueteRepository.setAppUser(u.uid());
        paqueteRepository.updateDireccion(id, direccion);
        p.setDireccionDestino(direccion);
        return p;
    }


    @Transactional
    public PaqueteEntity updatePaquete(PaqueteEntity paqueteEntity, AuthUser actor) {
        paqueteRepository.setAppUser(actor.uid());
        PaqueteEntity paqueteExistente = paqueteRepository.findByIdForUpdate(paqueteEntity.getPaqueteId())
                .orElseThrow(() -> new NoSuchElementException("Paquete inexistente"));
        if (paqueteEntity.getCodigo() != null) {
            paqueteExistente.setCodigo(paqueteEntity.getCodigo());
        }
        if (paqueteEntity.getEstado() != null && !paqueteEntity.getEstado().equals(paqueteExistente.getEstado())) {
            if (!"En centro".equals(paqueteEntity.getEstado()) || !"Registrado".equals(paqueteExistente.getEstado()))
                throw new IllegalArgumentException("Use los endpoints de asignación y entrega para cambiar estado");
            eventoRepository.save(new EventoSeguimientoEntity(null, paqueteExistente.getPaqueteId(), "En centro", "Recibido en centro", LocalDateTime.now()));
            paqueteExistente.setEstado("En centro");
        }
        paqueteExistente.setPeso(paqueteEntity.getPeso());
        paqueteExistente.setVolumen(paqueteEntity.getVolumen());
        paqueteExistente.setDireccionDestino(paqueteEntity.getDireccionDestino());
        paqueteExistente.setFechaCompromiso(paqueteEntity.getFechaCompromiso());
        paqueteExistente.setUsuarioClienteId(paqueteEntity.getUsuarioClienteId());
        paqueteExistente.setCentroDistId(paqueteEntity.getCentroDistId());
        int filasAfectadas = paqueteRepository.update(paqueteExistente);
        if (filasAfectadas == 0) {
            throw new IllegalStateException("No se pudo actualizar el paquete");
        }
        return paqueteExistente;
    }


    @Transactional
    public void deleteById(Integer id) {
        findById(id);
        int filasAfectadas = paqueteRepository.deleteById(id);
        if(filasAfectadas == 0) {
            throw new IllegalStateException("No se pudo eliminar el paquete");
        }
    }
    public TrackingResponse rastrear(String codigo, AuthUser u) {
        PaqueteEntity p = paqueteRepository.findByCodigo(codigo)
                .orElseThrow(() -> new NoSuchElementException("Paquete inexistente"));
        if (!paqueteRepository.esVisible(p.getPaqueteId(), u.rol(), u.uid())) {
            throw new AccessDeniedException("Paquete de otro usuario");
        }
        var eventos = eventoRepository.rastreoPaquete(codigo).stream()
                .map(e -> new EventoView(e.getEstado(), e.getDescripcion(), e.getFecha())).toList();
        return new TrackingResponse(p.getCodigo(), p.getEstado(), p.getDireccionDestino(),
                p.getFechaCompromiso(), p.getEntregadoAt(), p.getIntentos(), p.getFechaReprogramada(), eventos);
    }

    @Transactional
    public PaqueteEntity registrarFallida(Integer paqueteId, Integer rutaId, String motivo, AuthUser usuario) {
        verificarRutaPropia(rutaId, usuario);
        paqueteRepository.registrarFallida(paqueteId, rutaId, motivo);
        return findById(paqueteId);
    }

    @Transactional
    public PaqueteEntity registrarEntregado(Integer paqueteId, Integer rutaId, AuthUser usuario) {
        verificarRutaPropia(rutaId, usuario);
        paqueteRepository.registrarExitosa(paqueteId, rutaId);
        return findById(paqueteId);
    }

    private void verificarRutaPropia(Integer rutaId, AuthUser usuario) {
        if (rutaRepository.findById(rutaId).isEmpty()) {
            throw new NoSuchElementException("Ruta no encontrada");
        }
        if (!rutaRepository.esDeUsuario(rutaId, usuario.uid())) {
            throw new AccessDeniedException("La ruta no pertenece al repartidor");
        }
    }
}
