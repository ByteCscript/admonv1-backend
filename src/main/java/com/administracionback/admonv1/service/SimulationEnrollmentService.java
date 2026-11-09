package com.administracionback.admonv1.service;

import com.administracionback.admonv1.model.Application;
import com.administracionback.admonv1.model.ApplicationStatus;
import com.administracionback.admonv1.model.Call;
import com.administracionback.admonv1.model.User;
import com.administracionback.admonv1.repository.ApplicationRepository;
import com.administracionback.admonv1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/*
 * Entorno de simulación masiva (CU 1.1.2).
 *
 * Cada convocatoria nueva recibe una postulación PENDING_VALIDATION por cada usuario
 * simulado del seed (500 usuarios). Así el sorteo tiene participantes reales
 * sin depender de cargas manuales.
 *
 * Se ejecuta dentro de la transacción de createCall (MANDATORY): si la
 * convocatoria falla, la preinscripción también se revierte.
 */
@Service
@RequiredArgsConstructor
public class SimulationEnrollmentService {

    @Value("${simulation.auto-enroll:true}")
    private boolean autoEnroll;

    private static final String SIMULATED_REVIEWER = "admin@test.com";
    private static final ZoneId COLOMBIA_ZONE = ZoneId.of("America/Bogota");

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public int enrollSimulated(Call call) {

        if (!autoEnroll) {
            return 0;
        }

        List<User> simulated = userRepository.findSimulatedWithApartment();

        if (simulated.isEmpty()) {
            return 0;
        }

        LocalDateTime base = LocalDateTime.now(COLOMBIA_ZONE);
        List<Application> applications = new ArrayList<>(simulated.size());

        int index = 0;

        for (User user : simulated) {

            index++;

            Application application = new Application();

            application.setApplicationNumber(
                    "SIM-" + call.getId() + "-"
                            + String.format("%03d", index)
            );
            application.setResident(user);
            application.setApartment(user.getApartment());
            application.setCall(call);

            /*
             * N2/N3: los primeros 5 simulados se quedan PENDING_VALIDATION (sin revisar);
             * el resto (200) se crea directamente APPROVED con auditoría de simulación.
             * N5: nunca se toca Call.totalSlots aquí.
             */
            LocalDateTime createdAt = base.minusSeconds(index);

            if (index <= 5) {
                application.setStatus(ApplicationStatus.PENDING_VALIDATION);
                application.setReviewedBy(null);
                application.setReviewedAt(null);
            } else {
                application.setStatus(ApplicationStatus.APPROVED);
                application.setReviewedBy(SIMULATED_REVIEWER);
                application.setReviewedAt(createdAt);
            }

            application.setCreatedAt(createdAt);
            application.setVersion(0L);

            applications.add(application);
        }

        applicationRepository.saveAll(applications);

        return applications.size();
    }
}
