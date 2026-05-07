package ao.allon.kubata.faturacao.domain.enums;

public enum StatusFatura {
    RASCUNHO,
    EMITIDA,
    PAGA,
    CANCELADA,
    PROCESSADA,
    NULA  // Documento reservado para completar sequência (SAF-T AO compliant)
}
