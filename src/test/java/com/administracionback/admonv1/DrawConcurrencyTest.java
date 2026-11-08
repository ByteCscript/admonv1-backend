package com.administracionback.admonv1;

import com.administracionback.admonv1.model.Application;
import com.administracionback.admonv1.model.ApplicationStatus;
import com.administracionback.admonv1.model.Call;
import com.administracionback.admonv1.model.CallStatus;
import com.administracionback.admonv1.model.User;
import com.administracionback.admonv1.repository.ApplicationRepository;
import com.administracionback.admonv1.repository.CallDrawRepository;
import com.administracionback.admonv1.repository.CallRepository;
import com.administracionback.admonv1.repository.UserRepository;
import com.administracionback.admonv1.service.IDrawService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * C.U-08 · FE-03 y RNF-03: aunque dos administradores ejecuten el sorteo al
 * mismo tiempo, queda un único sorteo y las posiciones se asignan una sola
 * vez.
 *
 * No es @Transactional: cada ejecución confirma su propia transacción, como
 * en producción. Crea su propia convocatoria y descarta el contexto al
 * terminar para no afectar a otras pruebas.
 */
@SpringBootTest(properties = "jwt.secret=" + DrawIntegrationTest.TEST_JWT_SECRET)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DrawConcurrencyTest {

    private static final int SLOTS = 2;
    private static final int PARTICIPANTS = 8;

    @Autowired
    private IDrawService drawService;

    @Autowired
    private CallRepository callRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private CallDrawRepository callDrawRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void simultaneousExecutionsRegisterASingleDraw() throws Exception {
        Long callId = transactionTemplate.execute(status -> createClosedCallWithApprovedApplications());

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return drawService.executeDraw(callId, "admin@test.com").getStatusCode().value();
            }));
        }

        start.countDown();

        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> result : results) {
            statuses.add(result.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertEquals(List.of(201, 409), statuses.stream().sorted().toList(),
                "Una ejecución debe registrar el sorteo y la otra recibir 409");

        assertTrue(callDrawRepository.existsByCallId(callId));

        List<Application> drawn = applicationRepository
                .findByCallIdAndDrawPositionIsNotNullOrderByDrawPositionAsc(callId);
        assertEquals(PARTICIPANTS, drawn.size());
        for (int i = 0; i < drawn.size(); i++) {
            assertEquals(i + 1, drawn.get(i).getDrawPosition());
            assertEquals(i < SLOTS ? ApplicationStatus.SELECTED : ApplicationStatus.NOT_SELECTED,
                    drawn.get(i).getStatus());
        }
    }

    private Long createClosedCallWithApprovedApplications() {
        Call call = new Call();
        call.setTitle("Convocatoria de prueba de concurrencia");
        call.setDescription("C.U-08 FE-03");
        call.setStartDate(LocalDate.of(2026, 1, 1));
        call.setEndDate(LocalDate.of(2026, 1, 31));
        call.setAvailableSlots(SLOTS);
        call.setStatus(CallStatus.CLOSED);
        callRepository.save(call);

        User resident = userRepository.findByEmail("residente1@test.com").orElseThrow();

        for (int i = 0; i < PARTICIPANTS; i++) {
            Application application = new Application();
            application.setApplicationNumber("POST-" + UUID.randomUUID());
            application.setResident(resident);
            application.setApartment(resident.getApartment());
            application.setCall(call);
            application.setStatus(ApplicationStatus.APPROVED);
            application.setCreatedAt(LocalDateTime.now());
            applicationRepository.save(application);
        }

        return call.getId();
    }
}
