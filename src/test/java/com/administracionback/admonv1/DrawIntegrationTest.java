package com.administracionback.admonv1;

import com.administracionback.admonv1.model.Application;
import com.administracionback.admonv1.model.ApplicationStatus;
import com.administracionback.admonv1.model.Call;
import com.administracionback.admonv1.model.Document;
import com.administracionback.admonv1.model.DocumentStatus;
import com.administracionback.admonv1.model.DocumentType;
import com.administracionback.admonv1.repository.ApplicationRepository;
import com.administracionback.admonv1.repository.CallDrawRepository;
import com.administracionback.admonv1.repository.CallRepository;
import com.administracionback.admonv1.repository.DocumentRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * C.U-08 · Ejecutar sorteo de cupos, de punta a punta por HTTP.
 *
 * Usa src/test/resources/draw-test-data.sql: la convocatoria 4 está cerrada,
 * tiene 3 cupos, 21 solicitudes APPROVED, 2 CANCELLED y 1 REJECTED. Cada
 * prueba se revierte al terminar (@Transactional), así que todas parten del
 * mismo estado. Los tokens se obtienen iniciando sesión, igual que en
 * producción.
 */
@SpringBootTest(properties = {
        "jwt.secret=" + DrawIntegrationTest.TEST_JWT_SECRET,
        DrawIntegrationTest.TEST_DATA
})
@AutoConfigureMockMvc
@Transactional
class DrawIntegrationTest {

    /* Clave HS256 solo para pruebas (base64 de 35 bytes). */
    static final String TEST_JWT_SECRET = "dGVzdC1zZWNyZXQtZm9yLWRyYXctdGVzdHMtMzJieXRlcyE=";

    /* Datos propios de las pruebas, independientes del data.sql de la demo. */
    static final String TEST_DATA = "spring.sql.init.data-locations=classpath:draw-test-data.sql";

    private static final long DRAW_CALL = 4;
    private static final long OPEN_CALL = 2;
    private static final long CLOSED_CALL_WITHOUT_APPROVED = 1;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private CallRepository callRepository;

    @Autowired
    private CallDrawRepository callDrawRepository;

    @Autowired
    private DocumentRepository documentRepository;

    private String adminToken;
    private String residentToken;

    @BeforeEach
    void login() throws Exception {
        adminToken = token("admin@test.com");
        residentToken = token("residente1@test.com");
    }

    // ---------- Paso 2: resumen previo ----------

    @Test
    void previewShowsParticipantsAndOtherApplications() throws Exception {
        asAdmin(get("/api/calls/{id}/draw/preview", DRAW_CALL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSlots").value(3))
                .andExpect(jsonPath("$.data.approvedCount").value(21))
                .andExpect(jsonPath("$.data.unvalidatedCount").value(0))
                .andExpect(jsonPath("$.data.applicationsByStatus.REJECTED").value(1))
                .andExpect(jsonPath("$.data.applicationsByStatus.CANCELLED").value(2))
                .andExpect(jsonPath("$.data.drawExecuted").value(false))
                .andExpect(jsonPath("$.data.canExecute").value(true))
                .andExpect(jsonPath("$.data.blockingError").value(nullValue()))
                .andExpect(jsonPath("$.data.directAssignment").value(false));
    }

    // ---------- Flujo básico ----------

    @Test
    void executesDrawAssigningSlotsAndWaitlist() throws Exception {
        String body = asAdmin(post("/api/calls/{id}/draw", DRAW_CALL))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.participantsCount").value(21))
                .andExpect(jsonPath("$.data.selectedCount").value(3))
                .andExpect(jsonPath("$.data.waitlistCount").value(18))
                .andExpect(jsonPath("$.data.executedBy").value("admin@test.com"))
                .andExpect(jsonPath("$.data.algorithm").value("FISHER_YATES_SHA256_V1"))
                .andExpect(jsonPath("$.data.verified").value(true))
                .andExpect(jsonPath("$.data.inputApplicationNumbers", hasSize(21)))
                .andExpect(jsonPath("$.data.entries", hasSize(21)))
                .andExpect(jsonPath("$.data.entries[0].position").value(1))
                .andExpect(jsonPath("$.data.entries[0].status").value("SELECTED"))
                .andExpect(jsonPath("$.data.entries[0].waitlistPosition").value(nullValue()))
                .andExpect(jsonPath("$.data.entries[2].status").value("SELECTED"))
                .andExpect(jsonPath("$.data.entries[3].status").value("NOT_SELECTED"))
                .andExpect(jsonPath("$.data.entries[3].waitlistPosition").value(1))
                .andExpect(jsonPath("$.data.entries[20].position").value(21))
                .andExpect(jsonPath("$.data.entries[20].waitlistPosition").value(18))
                .andReturn().getResponse().getContentAsString();

        // La lista de entrada solo contiene aprobadas, en orden alfabético.
        List<String> input = JsonPath.read(body, "$.data.inputApplicationNumbers");
        assertEquals(input.stream().sorted().toList(), input);

        // RN-20: posiciones únicas 1..21 en las aprobadas.
        List<Application> drawn = applicationRepository
                .findByCallIdAndDrawPositionIsNotNullOrderByDrawPositionAsc(DRAW_CALL);
        assertEquals(21, drawn.size());
        for (int i = 0; i < drawn.size(); i++) {
            assertEquals(i + 1, drawn.get(i).getDrawPosition());
        }

        // Las no aprobadas no participan y conservan su estado.
        assertEquals(2, applicationRepository
                .findByCallIdAndStatus(DRAW_CALL, ApplicationStatus.CANCELLED).size());
        assertEquals(1, applicationRepository
                .findByCallIdAndStatus(DRAW_CALL, ApplicationStatus.REJECTED).size());
        applicationRepository.findByCallIdAndStatus(DRAW_CALL, ApplicationStatus.CANCELLED)
                .forEach(application -> assertNull(application.getDrawPosition()));

        // RN-19: el cierre queda definitivo.
        assertTrue(callRepository.findById(DRAW_CALL).orElseThrow().isClosedManually());
    }

    // ---------- Paso 13 y FA-03: consultar el resultado ----------

    @Test
    void resultMatchesTheExecutedDraw() throws Exception {
        String executed = asAdmin(post("/api/calls/{id}/draw", DRAW_CALL))
                .andReturn().getResponse().getContentAsString();

        List<String> order = JsonPath.read(executed, "$.data.entries[*].applicationNumber");
        String seed = JsonPath.read(executed, "$.data.seed");

        asAdmin(get("/api/calls/{id}/draw", DRAW_CALL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seed").value(seed))
                .andExpect(jsonPath("$.data.verified").value(true))
                .andExpect(jsonPath("$.data.entries[*].applicationNumber",
                        contains(order.toArray())));

        asAdmin(get("/api/calls/{id}/draw/preview", DRAW_CALL))
                .andExpect(jsonPath("$.data.drawExecuted").value(true))
                .andExpect(jsonPath("$.data.canExecute").value(false))
                .andExpect(jsonPath("$.data.blockingError").value("DRAW_ALREADY_EXECUTED"));
    }

    @Test
    void resultIsNotFoundBeforeTheDraw() throws Exception {
        asAdmin(get("/api/calls/{id}/draw", DRAW_CALL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DRAW_NOT_FOUND"));
    }

    // ---------- RN-21: verificación ----------

    @Test
    void verificationDetectsTamperedPositions() throws Exception {
        asAdmin(post("/api/calls/{id}/draw", DRAW_CALL)).andExpect(status().isCreated());

        List<Application> drawn = applicationRepository
                .findByCallIdAndDrawPositionIsNotNullOrderByDrawPositionAsc(DRAW_CALL);
        Application first = drawn.get(0);
        Application last = drawn.get(drawn.size() - 1);
        int firstPosition = first.getDrawPosition();
        first.setDrawPosition(last.getDrawPosition());
        last.setDrawPosition(firstPosition);
        applicationRepository.saveAllAndFlush(List.of(first, last));

        asAdmin(get("/api/calls/{id}/draw", DRAW_CALL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verified").value(false));
    }

    // ---------- Flujos de excepción ----------

    @Test
    void drawCannotBeRepeated() throws Exception {
        asAdmin(post("/api/calls/{id}/draw", DRAW_CALL)).andExpect(status().isCreated());

        asAdmin(post("/api/calls/{id}/draw", DRAW_CALL))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DRAW_ALREADY_EXECUTED"));
    }

    @Test
    void openCallCannotBeDrawn() throws Exception {
        asAdmin(post("/api/calls/{id}/draw", OPEN_CALL))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CALL_NOT_CLOSED"));
    }

    @Test
    void callWithoutApprovedApplicationsCannotBeDrawn() throws Exception {
        asAdmin(post("/api/calls/{id}/draw", CLOSED_CALL_WITHOUT_APPROVED))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("NO_APPROVED_APPLICATIONS"));
    }

    // ---------- RN-17: los cupos alcanzan para todas las aprobadas ----------

    @Test
    void approvedEqualToSlotsAreAllAssigned() throws Exception {
        assertDirectAssignment(21, 0);
    }

    @Test
    void approvedFewerThanSlotsAreAllAssignedAndTheRestStayUnassigned() throws Exception {
        assertDirectAssignment(30, 9);
    }

    private void assertDirectAssignment(int slots, int unassigned) throws Exception {
        Call call = callRepository.findById(DRAW_CALL).orElseThrow();
        call.setTotalSlots(slots);
        callRepository.saveAndFlush(call);

        asAdmin(get("/api/calls/{id}/draw/preview", DRAW_CALL))
                .andExpect(jsonPath("$.data.canExecute").value(true))
                .andExpect(jsonPath("$.data.directAssignment").value(true));

        asAdmin(post("/api/calls/{id}/draw", DRAW_CALL))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalSlots").value(slots))
                .andExpect(jsonPath("$.data.selectedCount").value(21))
                .andExpect(jsonPath("$.data.waitlistCount").value(0))
                .andExpect(jsonPath("$.data.directAssignment").value(true))
                .andExpect(jsonPath("$.data.verified").value(true))
                .andExpect(jsonPath("$.data.entries[20].status").value("SELECTED"))
                .andExpect(jsonPath("$.data.entries[20].waitlistPosition").value(nullValue()));

        assertEquals(21, applicationRepository
                .findByCallIdAndStatus(DRAW_CALL, ApplicationStatus.SELECTED).size());
        assertEquals(unassigned, callDrawRepository
                .findByCallId(DRAW_CALL).orElseThrow().getUnassignedSlots());
        assertEquals(slots, callRepository.findById(DRAW_CALL).orElseThrow().getTotalSlots());
    }

    @Test
    void unknownCallIsNotFound() throws Exception {
        asAdmin(post("/api/calls/{id}/draw", 999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CALL_NOT_FOUND"));
    }

    @Test
    void invalidCallIdIsRejected() throws Exception {
        asAdmin(post("/api/calls/{id}/draw", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_CALL_ID"));
    }

    // ---------- FE-05: solo administradores ----------

    @Test
    void residentCannotExecuteOrSeeTheDraw() throws Exception {
        mockMvc.perform(post("/api/calls/{id}/draw", DRAW_CALL)
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/calls/{id}/draw/preview", DRAW_CALL)
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/calls/{id}/draw", DRAW_CALL)
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserIsRejected() throws Exception {
        mockMvc.perform(post("/api/calls/{id}/draw", DRAW_CALL))
                .andExpect(status().isUnauthorized());
    }

    // ---------- RN-19 en C.U-01: convocatoria sorteada ----------

    @Test
    void drawnCallCannotBeReopenedOrEdited() throws Exception {
        asAdmin(post("/api/calls/{id}/draw", DRAW_CALL)).andExpect(status().isCreated());

        asAdmin(patch("/api/calls/{id}/reopen", DRAW_CALL))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DRAW_ALREADY_EXECUTED"));

        asAdmin(put("/api/calls/{id}", DRAW_CALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Otra","description":"x","startDate":"2030-01-01",
                         "endDate":"2030-02-01","totalSlots":50}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DRAW_ALREADY_EXECUTED"));

        asAdmin(get("/api/calls/{id}", DRAW_CALL))
                .andExpect(jsonPath("$.data.drawExecuted").value(true))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    // ---------- Postcondición 5: el residente ve su resultado ----------

    @Test
    void residentSeesTheirDrawPosition() throws Exception {
        asAdmin(post("/api/calls/{id}/draw", DRAW_CALL)).andExpect(status().isCreated());

        // residente1@test.com es dueño de la solicitud 1 (data.sql).
        Application own = applicationRepository.findById(1L).orElseThrow();
        int position = own.getDrawPosition();
        boolean selected = position <= 3;

        ResultActions detail = mockMvc.perform(get("/api/applications/{id}", 1)
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.drawPosition").value(position))
                .andExpect(jsonPath("$.data.status").value(selected ? "SELECTED" : "NOT_SELECTED"));

        if (selected) {
            detail.andExpect(jsonPath("$.data.waitlistPosition").value(nullValue()));
        } else {
            detail.andExpect(jsonPath("$.data.waitlistPosition").value(position - 3));
        }
    }

    // ---------- Integración con la aprobación (C.U-07) ----------

    @Test
    void approvingEnablesTheDrawWithoutConsumingSlots() throws Exception {
        Application pending = pendingApplication();

        asAdmin(patch("/api/applications/{id}/accept", pending.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        asAdmin(get("/api/calls/{id}/draw/preview", DRAW_CALL))
                .andExpect(jsonPath("$.data.totalSlots").value(3))
                .andExpect(jsonPath("$.data.approvedCount").value(22))
                .andExpect(jsonPath("$.data.canExecute").value(true));

        String body = asAdmin(post("/api/calls/{id}/draw", DRAW_CALL))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.participantsCount").value(22))
                .andExpect(jsonPath("$.data.selectedCount").value(3))
                .andReturn().getResponse().getContentAsString();

        List<String> participants = JsonPath.read(body, "$.data.inputApplicationNumbers");
        assertTrue(participants.contains(pending.getApplicationNumber()));
    }

    @Test
    void revokingAnApprovalDoesNotChangeSlots() throws Exception {
        Application approved = applicationRepository
                .findByCallIdAndStatus(DRAW_CALL, ApplicationStatus.APPROVED).get(0);

        asAdmin(patch("/api/applications/{id}/reject", approved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Documento vencido\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        asAdmin(get("/api/calls/{id}/draw/preview", DRAW_CALL))
                .andExpect(jsonPath("$.data.totalSlots").value(3))
                .andExpect(jsonPath("$.data.approvedCount").value(20));
    }

    @Test
    void reviewIsClosedOnceTheCallIsDrawn() throws Exception {
        asAdmin(post("/api/calls/{id}/draw", DRAW_CALL)).andExpect(status().isCreated());

        Application pending = pendingApplication();

        asAdmin(patch("/api/applications/{id}/accept", pending.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CALL_ALREADY_DRAWN"));

        asAdmin(patch("/api/applications/{id}/reject", pending.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Documento vencido\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CALL_ALREADY_DRAWN"));
    }

    // ---------- Estado inicial y revisión previa al sorteo ----------

    @Test
    void newApplicationStartsPendingValidationAndCanBeCancelled() throws Exception {
        String documentIds = Arrays.stream(DocumentType.values())
                .filter(DocumentType::isRequired)
                .map(type -> "\"" + uploadedDocument(type) + "\"")
                .collect(Collectors.joining(","));

        String body = asResident(post("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"callId\":" + OPEN_CALL + ",\"documentIds\":[" + documentIds + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_VALIDATION"))
                .andReturn().getResponse().getContentAsString();

        Integer applicationId = JsonPath.read(body, "$.data.id");

        asResident(patch("/api/applications/{id}/cancel", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void drawIsBlockedUntilAllApplicationsAreReviewed() throws Exception {
        Application pending = pendingApplication();

        asAdmin(get("/api/calls/{id}/draw/preview", DRAW_CALL))
                .andExpect(jsonPath("$.data.unvalidatedCount").value(1))
                .andExpect(jsonPath("$.data.canExecute").value(false))
                .andExpect(jsonPath("$.data.blockingError").value("APPLICATIONS_PENDING_VALIDATION"));

        asAdmin(post("/api/calls/{id}/draw", DRAW_CALL))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("APPLICATIONS_PENDING_VALIDATION"));

        asAdmin(patch("/api/applications/{id}/reject", pending.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"SOAT vencido\"}"))
                .andExpect(status().isOk());

        asAdmin(post("/api/calls/{id}/draw", DRAW_CALL))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.participantsCount").value(21));
    }

    @Test
    void rejectingRequiresAReason() throws Exception {
        Application pending = pendingApplication();

        for (String body : List.of("{}", "{\"reason\":\"   \"}")) {
            asAdmin(patch("/api/applications/{id}/reject", pending.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("REJECTION_REASON_REQUIRED"));
        }

        assertEquals(ApplicationStatus.PENDING_VALIDATION,
                applicationRepository.findById(pending.getId()).orElseThrow().getStatus());
    }

    @Test
    void residentSeesTheRejectionReason() throws Exception {
        // residente1@test.com es dueño de la solicitud 1 (APPROVED).
        asAdmin(patch("/api/applications/{id}/reject", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Licencia de tránsito ilegible\"}"))
                .andExpect(status().isOk());

        asResident(get("/api/applications/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("Licencia de tránsito ilegible"));
    }

    // ---------- Resultados, huella y CSV del sorteo ejecutado ----------

    @Test
    void resultsHashAndCsvComeFromTheExecutedDraw() throws Exception {
        asAdmin(post("/api/calls/{id}/draw", DRAW_CALL)).andExpect(status().isCreated());

        String results = mockMvc.perform(get("/api/calls/{id}/draw/results", DRAW_CALL)
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results", hasSize(21)))
                .andExpect(jsonPath("$.data.results[0].drawPosition").value(1))
                .andExpect(jsonPath("$.data.results[0].result").value("SELECTED"))
                .andExpect(jsonPath("$.data.results[0].status").value("SELECTED"))
                .andExpect(jsonPath("$.data.results[3].result").value("NOT_SELECTED"))
                .andExpect(jsonPath("$.data.results[3].status").value("NOT_SELECTED"))
                .andReturn().getResponse().getContentAsString();

        String hash = JsonPath.read(results, "$.data.integrityHash.hashValue");

        byte[] firstExport = exportCsv();
        byte[] secondExport = exportCsv();

        assertArrayEquals(firstExport, secondExport);
        assertEquals(hash, HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(firstExport)));

        asAdmin(get("/api/calls/{id}/draw/hash", DRAW_CALL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hashValue").value(hash));
    }

    @Test
    void csvIsNotFoundBeforeTheDraw() throws Exception {
        asAdmin(get("/api/admin/calls/{id}/draw/export", DRAW_CALL))
                .andExpect(status().isNotFound());
    }

    // ---------- utilidades ----------

    /* Deja una solicitud de la convocatoria del sorteo pendiente de validación. */
    private Application pendingApplication() {
        Application application = applicationRepository
                .findByCallIdAndStatus(DRAW_CALL, ApplicationStatus.CANCELLED).get(0);
        application.setStatus(ApplicationStatus.PENDING_VALIDATION);
        return applicationRepository.saveAndFlush(application);
    }

    private UUID uploadedDocument(DocumentType type) {
        UUID id = UUID.randomUUID();

        Document document = new Document();
        document.setId(id);
        document.setOriginalName(type.name() + ".pdf");
        document.setContentType("application/pdf");
        document.setSize(1024L);
        document.setS3Key("pruebas/" + id);
        document.setBucket("bucket-pruebas");
        document.setCreatedAt(LocalDateTime.now());
        document.setUploadedAt(LocalDateTime.now());
        document.setStatus(DocumentStatus.UPLOADED);
        document.setDocumentType(type);
        documentRepository.saveAndFlush(document);

        return id;
    }

    private ResultActions asResident(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + residentToken));
    }

    private byte[] exportCsv() throws Exception {
        return asAdmin(get("/api/admin/calls/{id}/draw/export", DRAW_CALL))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + adminToken));
    }

    private String token(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.data.token");
    }
}
