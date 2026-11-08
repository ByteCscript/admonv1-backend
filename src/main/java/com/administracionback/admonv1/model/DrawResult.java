package com.administracionback.admonv1.model;

/*
 * Resultado del sorteo de cupos (CU 1.1.2).
 * SELECTED     → postulación seleccionada (pasa a APPROVED).
 * NOT_SELECTED → postulación no seleccionada (conserva su estado previo).
 */
public enum DrawResult {
    SELECTED,
    NOT_SELECTED
}
