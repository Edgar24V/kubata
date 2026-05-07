package ao.allon.kubata.faturacao.service.agt;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Dados necessários para gerar o hash e o QR Code da AGT.
 */
public record AGTInvoiceData(
    String invoiceNo,
    LocalDateTime invoiceDate,
    LocalDateTime systemEntryDate,
    String customerTaxID,
    BigDecimal totalAmount,
    BigDecimal taxAmount,
    String previousHash,
    String hash // Hash atual, se já gerado
) {}
