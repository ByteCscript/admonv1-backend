package com.administracionback.admonv1.model;

import java.time.LocalDate;
import java.time.ZoneId;

/*
 * Única regla que decide el estado de una convocatoria (RN-25 de la wiki):
 * CLOSED si se cerró manualmente o pasó su fecha de fin, SCHEDULED antes de
 * la fecha de inicio y OPEN en otro caso, en zona America/Bogota.
 *
 * La usan CallServiceImpl y el sorteo de cupos (C.U-08), para que ambos
 * coincidan en cuándo una convocatoria está cerrada.
 */
public final class CallStatusPolicy {

    public static final ZoneId COLOMBIA_ZONE = ZoneId.of("America/Bogota");

    private CallStatusPolicy() {
    }

    public static CallStatus derive(Call call) {
        return derive(call, LocalDate.now(COLOMBIA_ZONE));
    }

    public static CallStatus derive(Call call, LocalDate today) {

        if (call.isClosedManually() || today.isAfter(call.getEndDate())) {
            return CallStatus.CLOSED;
        }

        if (today.isBefore(call.getStartDate())) {
            return CallStatus.SCHEDULED;
        }

        return CallStatus.OPEN;
    }
}
