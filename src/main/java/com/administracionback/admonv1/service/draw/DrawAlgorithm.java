package com.administracionback.admonv1.service.draw;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

/*
 * Algoritmo reproducible del sorteo de cupos (C.U-08, RN-21).
 *
 * FISHER_YATES_SHA256_V1:
 *   1. La lista de entrada son los números de solicitud ordenados
 *      alfabéticamente (canonicalInput).
 *   2. Para i = n-1 hasta 1:
 *        h = SHA-256(seed || i como entero de 4 bytes big-endian)
 *        j = h (entero sin signo de 256 bits) mod (i + 1)
 *        intercambiar las posiciones i y j
 *   3. La lista resultante es el orden del sorteo: el índice 0 es la
 *      posición 1.
 *
 * Con la semilla y la lista de entrada guardadas en CallDraw, cualquier
 * persona puede repetir el cálculo, en Java o en otro lenguaje, y comprobar
 * que el resultado coincide. El sesgo del módulo es despreciable: el valor
 * de 256 bits supera a (i + 1) en más de 2^240 veces.
 *
 * Es una clase sin estado ni dependencias para poder probarla de forma
 * aislada; no cambiar su comportamiento sin cambiar NAME, porque los sorteos
 * registrados deben poder verificarse siempre con su versión.
 */
public final class DrawAlgorithm {

    public static final String NAME = "FISHER_YATES_SHA256_V1";

    public static final int SEED_BYTES = 32;

    private static final String SEPARATOR = "\n";

    private DrawAlgorithm() {
    }

    /* Orden del sorteo para la lista de entrada y la semilla dadas. */
    public static List<String> shuffle(List<String> canonicalInput, byte[] seed) {

        if (seed == null || seed.length == 0) {
            throw new IllegalArgumentException("La semilla no puede estar vacía");
        }

        List<String> order = new ArrayList<>(canonicalInput);

        for (int i = order.size() - 1; i > 0; i--) {
            Collections.swap(order, i, index(seed, i));
        }

        return order;
    }

    /* Lista de entrada canónica: sin repetidos y en orden alfabético. */
    public static List<String> canonicalInput(List<String> applicationNumbers) {
        return applicationNumbers.stream()
                .distinct()
                .sorted()
                .toList();
    }

    public static String serialize(List<String> applicationNumbers) {
        return String.join(SEPARATOR, applicationNumbers);
    }

    public static List<String> deserialize(String snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return List.of();
        }
        return List.of(snapshot.split(SEPARATOR, -1));
    }

    public static String sha256Hex(String value) {
        return HexFormat.of().formatHex(
                sha256().digest(value.getBytes(StandardCharsets.UTF_8))
        );
    }

    public static String toHex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    public static byte[] fromHex(String hex) {
        return HexFormat.of().parseHex(hex);
    }

    private static int index(byte[] seed, int step) {
        MessageDigest digest = sha256();
        digest.update(seed);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(step).array());

        return new BigInteger(1, digest.digest())
                .mod(BigInteger.valueOf(step + 1L))
                .intValueExact();
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // Todo JRE debe incluir SHA-256 (requisito de la especificación de Java).
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
