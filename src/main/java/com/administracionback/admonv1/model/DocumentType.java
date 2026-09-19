package com.administracionback.admonv1.model;

public enum DocumentType {
    LICENCIA_TRANSITO(
            "Licencia de tránsito",
            true,
            "Acredita que el vehículo está autorizado para circular.",
            2L * 1024 * 1024,
            "application/pdf"
    ),
    SOAT_VIGENTE(
            "SOAT Vigente",
            true,
            "Certifica que el vehículo cuenta con el Seguro Obligatorio de Accidentes de Tránsito vigente.",
            2L * 1024 * 1024,
            "application/pdf"
    ),
    REVISION_TECNICOMECANICA(
            "Revisión Técnico-mecánica vigente",
            false,
            "Certifica el buen estado mecánico y ambiental del vehículo.",
            2L * 1024 * 1024,
            "application/pdf"
    ),
    LICENCIA_CONDUCCION(
            "Licencia de conducción del residente",
            true,
            "Acredita que el residente está autorizado para conducir el vehículo.",
            2L * 1024 * 1024,
            "application/pdf"
    ),
    SOPORTE_TENENCIA(
            "Soporte de tenencia (Si el vehículo no está a nombre del residente)",
            false,
            "Acredita la relación del residente con el vehículo cuando no figura como propietario.",
            2L * 1024 * 1024,
            "application/pdf"
    );

    private final String label;
    private final boolean required;
    private final String purpose;
    private final long maxSizeBytes;
    private final String acceptedContentType;

    DocumentType(
            String label,
            boolean required,
            String purpose,
            long maxSizeBytes,
            String acceptedContentType
    ) {
        this.label = label;
        this.required = required;
        this.purpose = purpose;
        this.maxSizeBytes = maxSizeBytes;
        this.acceptedContentType = acceptedContentType;
    }

    public String getLabel() {
        return label;
    }

    public boolean isRequired() {
        return required;
    }

    public String getPurpose() {
        return purpose;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public String getAcceptedContentType() {
        return acceptedContentType;
    }
}
