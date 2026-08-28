package com.administracionback.admonv1.controller;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.CallDTO;
import com.administracionback.admonv1.service.ICallService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
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
}
