package ao.allon.kubata.faturacao.service.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceReportData(
    String invoiceNo,
    LocalDateTime invoiceDate,
    LocalDateTime systemEntryDate,
    String operator,
    String customerName,
    String customerNif,
    BigDecimal totalNet,
    BigDecimal totalTax,
    BigDecimal totalGross,
    BigDecimal totalDiscount,
    BigDecimal totalRetencao,
    String hash,
    String previousHash
) {}
