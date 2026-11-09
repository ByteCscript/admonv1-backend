package com.administracionback.admonv1.service.draw;

/*
 * Error de negocio del sorteo. Al ser RuntimeException, revierte la
 * transacción de DrawTransactions (C.U-08, FE-06).
 */
public class DrawException extends RuntimeException {

    private final DrawError error;

    public DrawException(DrawError error) {
        this(error, error.message());
    }

    public DrawException(DrawError error, String message) {
        super(message);
        this.error = error;
    }

    public DrawError error() {
        return error;
    }
}
