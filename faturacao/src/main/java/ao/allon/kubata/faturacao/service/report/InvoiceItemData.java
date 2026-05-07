package ao.allon.kubata.faturacao.service.report;

import java.math.BigDecimal;

public record InvoiceItemData(
    String code,
    String description,
    BigDecimal quantity,
    String unit,
    BigDecimal unitPrice,
    BigDecimal taxRate,
    BigDecimal total,
    String exemptionCode,
    String exemptionReason
) {
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTaxRate() { return taxRate; }
    public BigDecimal getTotal() { return total; }
    public String getExemptionCode() { return exemptionCode; }
    public String getExemptionReason() { return exemptionReason; }
}
