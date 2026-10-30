package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.CallDTO;
import com.administracionback.admonv1.dto.CallRequestDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ICallService {

    ResponseEntity<ApiResponse<List<CallDTO>>> getCalls();

    ResponseEntity<ApiResponse<CallDTO>> getCall(Long callId);

    ResponseEntity<ApiResponse<CallDTO>> createCall(CallRequestDTO request);

    ResponseEntity<ApiResponse<CallDTO>> updateCall(Long callId, CallRequestDTO request);

    ResponseEntity<ApiResponse<CallDTO>> closeCall(Long callId);

    ResponseEntity<ApiResponse<CallDTO>> reopenCall(Long callId);

    ResponseEntity<ApiResponse<CallDTO>> deleteCall(Long callId);
}
