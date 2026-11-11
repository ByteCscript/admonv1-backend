package com.administracionback.admonv1.service.draw;

import com.administracionback.admonv1.model.Application;

import java.nio.charset.StandardCharsets;
import java.util.List;

/*
 * CSV del resultado del sorteo (CU 1.1.2, D2/D3). Lo usan la ejecución, que
 * guarda su huella, y la exportación, para que ambas produzcan los mismos
 * bytes. Las filas deben llegar ordenadas por posición.
 */
public final class DrawCsv {

    private static final String HEADER = "application_id,application_number,"
            + "apartment_number,resident_name,resident_id,result,status,draw_position\n";

    private DrawCsv() {
    }

    public static byte[] build(List<Application> results) {

        StringBuilder builder = new StringBuilder(HEADER);

        for (Application application : results) {

            builder.append(field(application.getId())).append(",")
                    .append(field(application.getApplicationNumber())).append(",")
                    .append(field(application.getApartment().getNumber())).append(",")
                    .append(field(application.getResident().getName())).append(",")
                    .append(field(application.getResident().getId())).append(",")
                    .append(field(application.getDrawResult())).append(",")
                    .append(field(application.getStatus())).append(",")
                    .append(field(application.getDrawPosition())).append("\n");
        }

        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    /* RFC 4180: entre comillas si contiene coma, comilla o salto de línea. */
    private static String field(Object value) {

        if (value == null) {
            return "";
        }

        String text = String.valueOf(value);

        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }

        return text;
    }
}
