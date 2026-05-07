package ao.allon.kubata.faturacao.service.barcode;

/**
 * Tipos de códigos de barras suportados pelo sistema.
 * Renomeado de BarcodeType para evitar conflitos de classpath.
 */
public enum BarcodeTypeEnum {
    /**
     * QR Code (Quick Response Code).
     * Usado para faturas e dados complexos.
     */
    QR_CODE,

    /**
     * Code 128.
     * Usado para logística e identificação interna.
     * Suporta todos os caracteres ASCII.
     */
    CODE_128,

    /**
     * EAN-13.
     * Usado para produtos de varejo.
     * Requer 12 ou 13 dígitos numéricos.
     */
    EAN_13,

    /**
     * PDF-417.
     * Usado em documentos de identificação e cartões de embarque.
     */
    PDF_417
}
