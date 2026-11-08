package com.administracionback.admonv1.model;

public enum ApplicationStatus {
    REGISTERED,
    PENDING_VALIDATION,
    APPROVED,
    REJECTED,
    CANCELLED,
    /*
     * Resultado del sorteo de cupos (C.U-08, RN-20): la solicitud quedó en
     * las posiciones 1..cupos.
     */
    SELECTED,
    /*
     * Resultado del sorteo de cupos (C.U-08, RN-20): la solicitud quedó en
     * lista de espera, en el orden de su posición.
     */
    NOT_SELECTED
}
