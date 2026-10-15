package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Persiste os resultados da comunicação AGT numa transacção própria.
 * Nunca grava a instância destacada que foi usada para preparar a submissão.
 */
@Service
public class AgtSubmissionStatusService {

    private final FaturaRepository faturaRepository;

    public AgtSubmissionStatusService(FaturaRepository faturaRepository) {
        this.faturaRepository = faturaRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void atualizarEstado(Long faturaId, String estado, String codigoValidacao) {
        if (faturaId == null || estado == null || estado.isBlank()) {
            throw new IllegalArgumentException("ID da factura e estado AGT são obrigatórios.");
        }

        Fatura fatura = faturaRepository.findById(faturaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Factura " + faturaId + " não encontrada ao actualizar o estado AGT."));

        fatura.setAgtSubmissionStatus(estado.trim().toUpperCase(java.util.Locale.ROOT));
        if (codigoValidacao != null && !codigoValidacao.isBlank()) {
            fatura.setAgtValidationCode(codigoValidacao.trim());
        }
        if ("SIMULATED".equalsIgnoreCase(estado)) {
            fatura.setAgtValidationCode(null);
            fatura.setAgtSubmissionDate(null);
        } else {
            fatura.setAgtSubmissionDate(LocalDateTime.now());
        }
        faturaRepository.save(fatura);
    }
}
