package com.example.demo.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaginaTest {

    @Test
    void paginatedResultCarriesTotalHeader() {
        var res = new Pagina<>(List.of(1, 2), 7L).responder();
        assertEquals("7", res.getHeaders().getFirst("X-Total-Count"));
        assertEquals(List.of(1, 2), res.getBody());
    }

    @Test
    void unpagedResultHasNoTotalHeader() {
        var res = new Pagina<>(List.of(1, 2, 3), null).responder();
        assertFalse(res.getHeaders().containsHeader("X-Total-Count"));
        assertEquals(3, res.getBody().size());
    }
}
