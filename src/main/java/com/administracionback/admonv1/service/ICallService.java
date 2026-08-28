package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.CallDTO;
import com.administracionback.admonv1.dto.CallDetailDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ICallService {

    ResponseEntity<ApiResponse<List<CallDTO>>> getCalls();

    ResponseEntity<ApiResponse<CallDTO>> getCall(Long callId);
}
