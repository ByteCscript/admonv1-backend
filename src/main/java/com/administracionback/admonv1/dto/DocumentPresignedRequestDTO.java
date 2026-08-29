package com.administracionback.admonv1.dto;

public record DocumentPresignedRequestDTO(String fileName,
                                          String contentType,
                                          Long size
) {
}
