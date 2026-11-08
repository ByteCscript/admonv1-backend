package com.administracionback.admonv1.service.draw;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * C.U-08 · RN-21: el sorteo es reproducible y justo.
 */
class DrawAlgorithmTest {

    private static final List<String> INPUT = List.of(
            "POST-A", "POST-B", "POST-C", "POST-D", "POST-E", "POST-F", "POST-G");

    private static byte[] seed0to31() {
        byte[] seed = new byte[32];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = (byte) i;
        }
        return seed;
    }

    /*
     * Vector de referencia calculado con una implementación independiente en
     * Python (hashlib). Si esta prueba falla, el algoritmo cambió y los
     * sorteos registrados con FISHER_YATES_SHA256_V1 ya no se podrían
     * verificar: hay que crear una versión nueva en lugar de modificarlo.
     */
    @Test
    void matchesIndependentReferenceImplementation() {
        assertEquals(
                List.of("POST-D", "POST-C", "POST-E", "POST-F", "POST-B", "POST-A", "POST-G"),
                DrawAlgorithm.shuffle(INPUT, seed0to31()));

        assertEquals(
                "294ad93d38b091a443be2300eec1a9ba5d0c2292dd710cb10266c5044ed2fd35",
                DrawAlgorithm.sha256Hex(DrawAlgorithm.serialize(INPUT)));
    }

    @Test
    void sameSeedAndInputAlwaysGiveSameOrder() {
        assertEquals(
                DrawAlgorithm.shuffle(INPUT, seed0to31()),
                DrawAlgorithm.shuffle(INPUT, seed0to31()));
    }

    @Test
    void differentSeedsGiveDifferentOrders() {
        byte[] other = seed0to31();
        other[0] = 1;

        assertNotEquals(
                DrawAlgorithm.shuffle(INPUT, seed0to31()),
                DrawAlgorithm.shuffle(INPUT, other));
    }

    @Test
    void resultIsAPermutationOfTheInput() {
        List<String> order = DrawAlgorithm.shuffle(INPUT, seed0to31());

        assertEquals(INPUT.size(), order.size());
        assertEquals(DrawAlgorithm.canonicalInput(order), INPUT);
    }

    @Test
    void doesNotModifyTheInputList() {
        List<String> input = new ArrayList<>(INPUT);

        DrawAlgorithm.shuffle(input, seed0to31());

        assertEquals(INPUT, input);
    }

    @Test
    void canonicalInputIsSortedAndWithoutDuplicates() {
        assertEquals(
                List.of("POST-A", "POST-B", "POST-C"),
                DrawAlgorithm.canonicalInput(List.of("POST-C", "POST-A", "POST-B", "POST-A")));
    }

    @Test
    void serializeAndDeserializeAreInverse() {
        assertEquals(INPUT, DrawAlgorithm.deserialize(DrawAlgorithm.serialize(INPUT)));
        assertEquals(List.of(), DrawAlgorithm.deserialize(""));
    }

    @Test
    void handlesEmptyAndSingleInputs() {
        assertEquals(List.of(), DrawAlgorithm.shuffle(List.of(), seed0to31()));
        assertEquals(List.of("POST-A"), DrawAlgorithm.shuffle(List.of("POST-A"), seed0to31()));
    }

    @Test
    void rejectsEmptySeed() {
        assertThrows(IllegalArgumentException.class,
                () -> DrawAlgorithm.shuffle(INPUT, new byte[0]));
    }

    /*
     * Justicia: con 3 participantes, las 6 permutaciones deben salir con
     * frecuencias parecidas. Semillas deterministas para que la prueba no
     * sea intermitente; el margen (±20 %) es amplio frente a la desviación
     * esperada (~3 %).
     */
    @Test
    void allPermutationsAreRoughlyEquallyLikely() {
        List<String> input = List.of("A", "B", "C");
        int runs = 6_000;
        Map<List<String>, Integer> frequencies = new HashMap<>();

        for (int i = 0; i < runs; i++) {
            byte[] seed = ByteBuffer.allocate(32).putInt(i).array();
            frequencies.merge(DrawAlgorithm.shuffle(input, seed), 1, Integer::sum);
        }

        assertEquals(6, frequencies.size());

        frequencies.values().forEach(count ->
                assertTrue(count > 800 && count < 1_200,
                        "Frecuencia fuera de rango: " + frequencies));
    }
}
