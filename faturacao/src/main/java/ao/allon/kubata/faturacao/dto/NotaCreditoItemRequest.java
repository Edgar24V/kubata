package ao.allon.kubata.faturacao.dto;

import java.math.BigDecimal;

/**
 * Quantidade a creditar de uma linha específica da factura original.
 * A quantidade é expressa na unidade comercial original; o serviço valida
 * disponibilidade, precisão decimal e devoluções de stock antes de emitir.
 */
public record NotaCreditoItemRequest(Long itemOriginalId, BigDecimal quantidade) {
    public NotaCreditoItemRequest {
        if (itemOriginalId == null || itemOriginalId <= 0) {
            throw new IllegalArgumentException("A linha original da nota de crédito é obrigatória.");
        }
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new IllegalArgumentException("A quantidade a creditar deve ser maior que zero.");
        }
        BigDecimal stripped = quantidade.stripTrailingZeros();
        if (stripped.scale() > 3) {
            throw new IllegalArgumentException("A quantidade a creditar admite no máximo três casas decimais.");
        }
    }
}
