package com.administracionback.admonv1.service;


import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.CallDTO;
import com.administracionback.admonv1.model.Call;
import com.administracionback.admonv1.repository.CallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CallServiceImpl implements ICallService {
    private final CallRepository callRepository;

    @Override
    public ResponseEntity<ApiResponse<List<CallDTO>>> getCalls() {

        List<CallDTO> calls = callRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();

        ApiResponse<List<CallDTO>> response =
                new ApiResponse<>(
                        "Convocatorias consultadas correctamente",
                        calls,
                        null
                );

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ApiResponse<CallDTO>> getCall(Long callId) {

        if (callId == null || callId <= 0) {

            ApiResponse<CallDTO> response =
                    new ApiResponse<>(
                            "El identificador de la convocatoria es inválido",
                            null,
                            "INVALID_CALL_ID"
                    );

            return ResponseEntity
                    .badRequest()
                    .body(response);
        }

        return callRepository.findById(callId)
                .map(call -> {

                    CallDTO callDTO = mapToDTO(call);

                    ApiResponse<CallDTO> response =
                            new ApiResponse<>(
                                    "Convocatoria consultada correctamente",
                                    callDTO,
                                    null
                            );

                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {

                    ApiResponse<CallDTO> response =
                            new ApiResponse<>(
                                    "No se encontró la convocatoria",
                                    null,
                                    "CALL_NOT_FOUND"
                            );

                    return ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(response);
                });
    }

    private CallDTO mapToDTO(Call call) {
        return new CallDTO(
                call.getId(),
                call.getTitle(),
                call.getAvailableSlots()
        );
    }
}

