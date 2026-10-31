package com.administracionback.admonv1.controller;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.CallDTO;
import com.administracionback.admonv1.dto.CallRequestDTO;
import com.administracionback.admonv1.service.ICallService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CallController {



    private final ICallService iCallService;


    @GetMapping
    public ResponseEntity<ApiResponse<List<CallDTO>>> getCalls() {
        return iCallService.getCalls();
    }

    @GetMapping("/{callId}")
    public ResponseEntity<ApiResponse<CallDTO>> getCall(
            @PathVariable Long callId) {

        return iCallService.getCall(callId);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CallDTO>> createCall(
            @RequestBody CallRequestDTO request) {

        return iCallService.createCall(request);
    }

    @PutMapping("/{callId}")
    public ResponseEntity<ApiResponse<CallDTO>> updateCall(
            @PathVariable Long callId,
            @RequestBody CallRequestDTO request) {

        return iCallService.updateCall(callId, request);
    }

    @PatchMapping("/{callId}/close")
    public ResponseEntity<ApiResponse<CallDTO>> closeCall(
            @PathVariable Long callId) {

        return iCallService.closeCall(callId);
    }

    @PatchMapping("/{callId}/reopen")
    public ResponseEntity<ApiResponse<CallDTO>> reopenCall(
            @PathVariable Long callId) {

        return iCallService.reopenCall(callId);
    }

    @DeleteMapping("/{callId}")
    public ResponseEntity<ApiResponse<CallDTO>> deleteCall(
            @PathVariable Long callId) {

        return iCallService.deleteCall(callId);
    }
}
