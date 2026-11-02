package com.example.demo.dto;

import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * List result of a CRUD query. {@code total} is the filtered row count and is only set when the caller asked for
 * a page; an unpaged result carries the full list and no total.
 */
public record Pagina<T>(List<T> items, Long total) {

    public static final String TOTAL_HEADER = "X-Total-Count";

    /** The body stays a plain JSON array; the total travels in {@code X-Total-Count} when paginated. */
    public ResponseEntity<List<T>> responder() {
        ResponseEntity.BodyBuilder ok = ResponseEntity.ok();
        if (total != null) {
            ok.header(TOTAL_HEADER, String.valueOf(total));
        }
        return ok.body(items);
    }
}
