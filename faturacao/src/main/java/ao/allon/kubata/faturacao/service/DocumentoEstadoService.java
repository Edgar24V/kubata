package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.enums.EstadoDocumento;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Serviço para gestão de estados de documentos fiscais conforme normas SAF-T-AO (AGT Angola).
 *
 * Implementa a lógica de:
 * - Original (O): Documento emitido pela primeira vez
 * - Duplicado (D): Cópia para o emitente
 * - Segunda Via (S): Cópia para o cliente
 * - Emissão em Terceiros (E): Documento emitido por terceiros
 *
 * @see ao.allon.kubata.faturacao.domain.enums.EstadoDocumento
 */
@Service
public class DocumentoEstadoService {

    private final FaturaRepository faturaRepository;
    private final WebhookEventPublisher webhookEventPublisher;

    public DocumentoEstadoService(
            FaturaRepository faturaRepository,
            WebhookEventPublisher webhookEventPublisher) {
        this.faturaRepository = faturaRepository;
        this.webhookEventPublisher = webhookEventPublisher;
    }

    @Transactional
    public Fatura gerarDuplicado(Fatura faturaOriginal, Long usuarioId, String motivo) {
        if (faturaOriginal.getEstadoDocumento() == EstadoDocumento.DUPLICADO) {
            throw new IllegalStateException("Não é possível gerar duplicado de um documento que já é duplicado.");
        }

        Fatura duplicado = criarCopia(faturaOriginal, EstadoDocumento.DUPLICADO, usuarioId, motivo);
        duplicado.setNumero(faturaOriginal.getNumero() + "-D");

        Fatura saved = faturaRepository.save(duplicado);
        webhookEventPublisher.publish(
                "document.copy.created",
                Map.of(
                        "documentId", saved.getId(),
                        "documentNumber", saved.getNumero(),
                        "copyType", "DUPLICADO",
                        "originalDocumentId", faturaOriginal.getId()
                ),
                null
        );
        return saved;
    }

    @Transactional
    public Fatura gerarSegundaVia(Fatura faturaOriginal, Long usuarioId, String motivo) {
        if (faturaOriginal.getEstadoDocumento() == EstadoDocumento.SEGUNDA_VIA) {
            throw new IllegalStateException("Não é possível gerar segunda via de um documento que já é segunda via.");
        }

        Fatura segundaVia = criarCopia(faturaOriginal, EstadoDocumento.SEGUNDA_VIA, usuarioId, motivo);
        segundaVia.setNumero(faturaOriginal.getNumero() + "-S");

        Fatura saved = faturaRepository.save(segundaVia);
        webhookEventPublisher.publish(
                "document.copy.created",
                Map.of(
                        "documentId", saved.getId(),
                        "documentNumber", saved.getNumero(),
                        "copyType", "SEGUNDA_VIA",
                        "originalDocumentId", faturaOriginal.getId()
                ),
                null
        );
        return saved;
    }

    @Transactional
    public Fatura registarEmissaoTerceiros(Fatura fatura, String terceiroIdentificacao, Long usuarioId) {
        if (fatura.getStatus() != StatusFatura.EMITIDA) {
            throw new IllegalStateException("Apenas documentos emitidos podem ser marcados como emissão em terceiros.");
        }

        fatura.setEstadoDocumento(EstadoDocumento.EMISSAO_TERCEIROS);
        fatura.setDataGeracaoCopia(LocalDateTime.now());
        fatura.setUsuarioGeracaoCopiaId(usuarioId);
        fatura.setMotivoGeracaoCopia("Emissão em terceiros: " + terceiroIdentificacao);

        Fatura saved = faturaRepository.save(fatura);
        webhookEventPublisher.publish(
                "document.third_party_issuance.registered",
                Map.of(
                        "documentId", saved.getId(),
                        "documentNumber", saved.getNumero(),
                        "thirdParty", terceiroIdentificacao
                ),
                null
        );
        return saved;
    }

    public boolean podeAlterar(Fatura fatura) {
        if (fatura.getEstadoDocumento() != EstadoDocumento.ORIGINAL) {
            return false;
        }
        return fatura.getStatus() == StatusFatura.RASCUNHO || fatura.getStatus() == StatusFatura.EMITIDA;
    }

    public boolean podeAnular(Fatura fatura) {
        return fatura.getEstadoDocumento() == EstadoDocumento.ORIGINAL
                && fatura.getStatus() == StatusFatura.EMITIDA;
    }

    public String getIndicacaoVisual(Fatura fatura) {
        return fatura.getEstadoDocumento().getTextoDocumento();
    }

    public boolean requerIndicacaoVisual(Fatura fatura) {
        return fatura.getEstadoDocumento().requerIndicacaoVisual();
    }

    public String getCodigoSAFT(Fatura fatura) {
        return fatura.getEstadoDocumento().getCodigo();
    }

    private Fatura criarCopia(Fatura original, EstadoDocumento estado, Long usuarioId, String motivo) {
        Fatura copia = new Fatura();

        copia.setTipoDocumento(original.getTipoDocumento());
        copia.setSerie(original.getSerie());
        copia.setCliente(original.getCliente());
        copia.setDataEmissao(original.getDataEmissao());
        copia.setHoraEmissao(original.getHoraEmissao());
        copia.setDataVencimento(original.getDataVencimento());
        copia.setSubtotal(original.getSubtotal());
        copia.setIva(original.getIva());
        copia.setTotal(original.getTotal());
        copia.setTotalRetencao(original.getTotalRetencao());
        copia.setStatus(original.getStatus());
        copia.setMetodoPagamento(original.getMetodoPagamento());
        copia.setObservacoes(original.getObservacoes());
        copia.setHash(original.getHash());
        copia.setHashControl(original.getHashControl());
        copia.setModoFormacao(original.getModoFormacao());

        copia.setEstadoDocumento(estado);
        copia.setFaturaOriginal(original);
        copia.setDataGeracaoCopia(LocalDateTime.now());
        copia.setUsuarioGeracaoCopiaId(usuarioId);
        copia.setMotivoGeracaoCopia(motivo);

        original.getItens().forEach(item -> {
            // Criar nova instância do item para a cópia
            // (A implementação depende da estrutura de ItemFatura)
        });

        return copia;
    }

    public Optional<Fatura> findOriginal(String numero) {
        String numeroBase = numero.replaceAll("-[DS]$", "");

        return faturaRepository.findByNumero(numeroBase)
                .filter(f -> f.getEstadoDocumento() == EstadoDocumento.ORIGINAL);
    }

    public long countCopias(Fatura faturaOriginal) {
        return faturaRepository.findByFaturaOriginal(faturaOriginal).size();
    }

    public boolean validarConformidadeAGT(Fatura fatura) {
        if (fatura.getEstadoDocumento() == null) {
            return false;
        }

        if (fatura.getEstadoDocumento() == EstadoDocumento.EMISSAO_TERCEIROS
                && (fatura.getMotivoGeracaoCopia() == null || fatura.getMotivoGeracaoCopia().isEmpty())) {
            return false;
        }

        if (fatura.getEstadoDocumento() != EstadoDocumento.ORIGINAL
                && fatura.getFaturaOriginal() == null) {
            return false;
        }

        return true;
    }
}
