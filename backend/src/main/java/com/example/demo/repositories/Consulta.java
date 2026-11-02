package com.example.demo.repositories;

import com.example.demo.dto.Pagina;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.ArrayList;
import java.util.List;

/** Shared list query: optional LIMIT/OFFSET with a COUNT over the same WHERE. Bind parameters only. */
final class Consulta {

    static final int TAMANO_POR_DEFECTO = 20;
    static final int TAMANO_MAXIMO = 100;

    private Consulta() {}

    /**
     * @param select  SELECT with its WHERE already applied (role visibility and filters), without ORDER BY
     * @param orderBy deterministic, whitelisted order (the PK); never built from user input
     * @param page    0-based page, or null for the full list
     * @param size    page size, default 20, clamped to 1..100
     */
    static <T> Pagina<T> listar(JdbcTemplate jdbc, String select, List<Object> args, String orderBy,
                                RowMapper<T> mapper, Integer page, Integer size) {
        if (page == null) {
            return new Pagina<>(jdbc.query(select + " ORDER BY " + orderBy, mapper, args.toArray()), null);
        }
        int tamano = Math.min(TAMANO_MAXIMO, Math.max(1, size == null ? TAMANO_POR_DEFECTO : size));
        long offset = (long) Math.max(0, page) * tamano;
        Long total = jdbc.queryForObject("SELECT count(*) FROM (" + select + ") filtrado", Long.class, args.toArray());
        List<Object> conLimite = new ArrayList<>(args);
        conLimite.add(tamano);
        conLimite.add(offset);
        List<T> items = jdbc.query(select + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?", mapper,
                conLimite.toArray());
        return new Pagina<>(items, total == null ? 0L : total);
    }

    /** Case-insensitive contains pattern with LIKE wildcards escaped (use with {@code ILIKE ? ESCAPE '\'}). */
    static String contiene(String texto) {
        return "%" + texto.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    static boolean hay(String texto) {
        return texto != null && !texto.isBlank();
    }
}
