package com.administracionback.admonv1.service.draw;

import org.springframework.http.HttpStatus;

/*
 * Errores de negocio del sorteo de cupos (C.U-08, flujos de excepción).
 */
public enum DrawError {

    INVALID_CALL_ID(HttpStatus.BAD_REQUEST,
            "El identificador de la convocatoria es inválido"),

    // FE-01
    CALL_NOT_FOUND(HttpStatus.NOT_FOUND,
            "No se encontró la convocatoria"),

    // FE-02, RN-18
    CALL_NOT_CLOSED(HttpStatus.CONFLICT,
            "La convocatoria debe estar cerrada para ejecutar el sorteo"),

    // FE-03, RN-19
    DRAW_ALREADY_EXECUTED(HttpStatus.CONFLICT,
            "La convocatoria ya tiene un sorteo ejecutado y el sorteo no se puede repetir"),

    // FE-04, RN-17
    INSUFFICIENT_APPROVED_APPLICATIONS(HttpStatus.CONFLICT,
            "Las solicitudes aprobadas deben superar el número de cupos de la convocatoria"),

    // Consulta del resultado de una convocatoria sin sorteo.
    DRAW_NOT_FOUND(HttpStatus.NOT_FOUND,
            "La convocatoria no tiene un sorteo ejecutado");

    private final HttpStatus status;
    private final String message;

    DrawError(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
