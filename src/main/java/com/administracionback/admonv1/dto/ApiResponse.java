package com.administracionback.admonv1.dto;

public record ApiResponse<T>(
        String message,
        T data,
        String error
) {
}
