package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.enums.EstadoDocumento;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    public DocumentoEstadoService(FaturaRepository faturaRepository) {
        this.faturaRepository = faturaRepository;
    }

    /**
     * Gera um duplicado do documento original.
     * O duplicado é uma cópia destinada ao emitente para arquivo.
     * 
     * @param faturaOriginal A fatura original
     * @param usuarioId ID do utilizador que está a gerar o duplicado
     * @param motivo Motivo da geração do duplicado
     * @return A nova fatura marcada como DUPLICADO
     */
    @Transactional
    public Fatura gerarDuplicado(Fatura faturaOriginal, Long usuarioId, String motivo) {
        if (faturaOriginal.getEstadoDocumento() == EstadoDocumento.DUPLICADO) {
            throw new IllegalStateException("Não é possível gerar duplicado de um documento que já é duplicado.");
        }

        Fatura duplicado = criarCopia(faturaOriginal, EstadoDocumento.DUPLICADO, usuarioId, motivo);
        
        // O duplicado mantém o mesmo número mas é marcado como D
        duplicado.setNumero(faturaOriginal.getNumero() + "-D");
        
        return faturaRepository.save(duplicado);
    }

    /**
     * Gera uma segunda via do documento original.
     * A segunda via é uma cópia destinada ao cliente/adquirente.
     * 
     * @param faturaOriginal A fatura original
     * @param usuarioId ID do utilizador que está a gerar a segunda via
     * @param motivo Motivo da geração da segunda via
     * @return A nova fatura marcada como SEGUNDA_VIA
     */
    @Transactional
    public Fatura gerarSegundaVia(Fatura faturaOriginal, Long usuarioId, String motivo) {
        if (faturaOriginal.getEstadoDocumento() == EstadoDocumento.SEGUNDA_VIA) {
            throw new IllegalStateException("Não é possível gerar segunda via de um documento que já é segunda via.");
        }

        Fatura segundaVia = criarCopia(faturaOriginal, EstadoDocumento.SEGUNDA_VIA, usuarioId, motivo);
        
        // A segunda via mantém o mesmo número mas é marcada como S
        segundaVia.setNumero(faturaOriginal.getNumero() + "-S");
        
        return faturaRepository.save(segundaVia);
    }

    /**
     * Regista um documento como emitido por terceiros.
     * Requer autorização prévia da AGT.
     * 
     * @param fatura A fatura a ser marcada
     * @param terceiroIdentificacao Identificação do terceiro emitente
     * @param usuarioId ID do utilizador
     * @return A fatura atualizada
     */
    @Transactional
    public Fatura registarEmissaoTerceiros(Fatura fatura, String terceiroIdentificacao, Long usuarioId) {
        if (fatura.getStatus() != StatusFatura.EMITIDA) {
            throw new IllegalStateException("Apenas documentos emitidos podem ser marcados como emissão em terceiros.");
        }

        fatura.setEstadoDocumento(EstadoDocumento.EMISSAO_TERCEIROS);
        fatura.setDataGeracaoCopia(LocalDateTime.now());
        fatura.setUsuarioGeracaoCopiaId(usuarioId);
        fatura.setMotivoGeracaoCopia("Emissão em terceiros: " + terceiroIdentificacao);

        return faturaRepository.save(fatura);
    }

    /**
     * Verifica se um documento pode ser alterado.
     * Apenas documentos ORIGINAL podem ser alterados antes de emitidos.
     * 
     * @param fatura A fatura a verificar
     * @return true se pode ser alterada, false caso contrário
     */
    public boolean podeAlterar(Fatura fatura) {
        if (fatura.getEstadoDocumento() != EstadoDocumento.ORIGINAL) {
            return false;
        }
        return fatura.getStatus() == StatusFatura.RASCUNHO || fatura.getStatus() == StatusFatura.EMITIDA;
    }

    /**
     * Verifica se um documento pode ser anulado.
     * Apenas documentos ORIGINAL emitidos podem ser anulados.
     * 
     * @param fatura A fatura a verificar
     * @return true se pode ser anulada, false caso contrário
     */
    public boolean podeAnular(Fatura fatura) {
        return fatura.getEstadoDocumento() == EstadoDocumento.ORIGINAL 
               && fatura.getStatus() == StatusFatura.EMITIDA;
    }

    /**
     * Obtém o texto de indicação visual para o documento.
     * Este texto deve ser exibido no PDF/impressão.
     * 
     * @param fatura A fatura
     * @return O texto de indicação ou string vazia se for original
     */
    public String getIndicacaoVisual(Fatura fatura) {
        return fatura.getEstadoDocumento().getTextoDocumento();
    }

    /**
     * Verifica se o documento requer indicação visual especial.
     * 
     * @param fatura A fatura
     * @return true se requer indicação visual
     */
    public boolean requerIndicacaoVisual(Fatura fatura) {
        return fatura.getEstadoDocumento().requerIndicacaoVisual();
    }

    /**
     * Obtém o código SAF-T-AO do estado do documento.
     * 
     * @param fatura A fatura
     * @return O código (O, D, S, E)
     */
    public String getCodigoSAFT(Fatura fatura) {
        return fatura.getEstadoDocumento().getCodigo();
    }

    /**
     * Cria uma cópia da fatura com o estado especificado.
     */
    private Fatura criarCopia(Fatura original, EstadoDocumento estado, Long usuarioId, String motivo) {
        Fatura copia = new Fatura();
        
        // Copiar dados básicos
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
        
        // Definir estado e referência
        copia.setEstadoDocumento(estado);
        copia.setFaturaOriginal(original);
        copia.setDataGeracaoCopia(LocalDateTime.now());
        copia.setUsuarioGeracaoCopiaId(usuarioId);
        copia.setMotivoGeracaoCopia(motivo);
        
        // Copiar itens
        original.getItens().forEach(item -> {
            // Criar nova instância do item para a cópia
            // (A implementação depende da estrutura de ItemFatura)
        });
        
        return copia;
    }

    /**
     * Busca o documento original pelo número.
     * 
     * @param numero O número do documento
     * @return O documento original (não cópia) ou empty se não encontrado
     */
    public Optional<Fatura> findOriginal(String numero) {
        // Remove sufixos de cópia (-D, -S) se existirem
        String numeroBase = numero.replaceAll("-[DS]$", "");
        
        return faturaRepository.findByNumero(numeroBase)
                .filter(f -> f.getEstadoDocumento() == EstadoDocumento.ORIGINAL);
    }

    /**
     * Conta quantas cópias (duplicados/segundas vias) existem de um documento.
     * 
     * @param faturaOriginal A fatura original
     * @return O número de cópias
     */
    public long countCopias(Fatura faturaOriginal) {
        return faturaRepository.findByFaturaOriginal(faturaOriginal).size();
    }

    /**
     * Valida se o documento está em conformidade com as normas AGT.
     * 
     * @param fatura A fatura a validar
     * @return true se está em conformidade
     */
    public boolean validarConformidadeAGT(Fatura fatura) {
        // Documentos devem ter um estado válido
        if (fatura.getEstadoDocumento() == null) {
            return false;
        }

        // Documentos emitidos em terceiros devem ter justificativa
        if (fatura.getEstadoDocumento() == EstadoDocumento.EMISSAO_TERCEIROS 
            && (fatura.getMotivoGeracaoCopia() == null || fatura.getMotivoGeracaoCopia().isEmpty())) {
            return false;
        }

        // Cópias devem referenciar o original
        if (fatura.getEstadoDocumento() != EstadoDocumento.ORIGINAL 
            && fatura.getFaturaOriginal() == null) {
            return false;
        }

        return true;
    }
}
