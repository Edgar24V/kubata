package ao.allon.kubata.faturacao.service.agt;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.enums.DocumentStatus;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.service.agt.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de mapeamento de Faturas para DTOs da API AGT.
 * Conforme Decreto Executivo n.º 683/25.
 */
@Service
public class AGTInvoiceMapperService {

    private final JWSDigitalSignatureService signatureService;

    @Value("${agt.software.product-id:Kubata ERP}")
    private String productId;

    @Value("${agt.software.version:1.0.0}")
    private String productVersion;

    @Value("${agt.software.validation-number:}")
    private String softwareValidationNumber;

    @Value("${agt.emitter.nif:}")
    private String emitterNif;

    @Value("${agt.emitter.company-name:}")
    private String emitterCompanyName;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public AGTInvoiceMapperService(JWSDigitalSignatureService signatureService) {
        this.signatureService = signatureService;
    }

    /**
     * Cria a requisição de submissão completa para a API AGT.
     */
    public AGTInvoiceSubmissionRequest createSubmissionRequest(List<Fatura> faturas) {
        String submissionGUID = UUID.randomUUID().toString();
        String timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        AGTInvoiceSubmissionRequest.SoftwareInfoDetail softwareInfoDetail = 
            new AGTInvoiceSubmissionRequest.SoftwareInfoDetail(productId, productVersion, softwareValidationNumber);
        
        AGTInvoiceSubmissionRequest.SoftwareInfo softwareInfo = 
            new AGTInvoiceSubmissionRequest.SoftwareInfo(softwareInfoDetail, ""); // JWS signature will be added later

        List<DocumentDTO> documents = new ArrayList<>();
        for (Fatura fatura : faturas) {
            documents.add(mapToDocumentDTO(fatura));
        }

        return new AGTInvoiceSubmissionRequest(
            submissionGUID,
            emitterNif,
            timestamp,
            softwareInfo,
            documents
        );
    }

    /**
     * Mapeia uma Fatura para DocumentDTO.
     */
    public DocumentDTO mapToDocumentDTO(Fatura fatura) {
        DocumentDTO dto = new DocumentDTO();
        
        // Identificação do documento
        dto.setDocumentNo(fatura.getNumero());
        dto.setDocumentType(getDocumentTypeCode(fatura.getTipoDocumento()));
        dto.setDocumentDate(fatura.getDataEmissao().format(DATE_FORMATTER));
        dto.setSystemEntryDate(fatura.getSystemEntryDate().format(DATE_TIME_FORMATTER));
        
        // Status do documento (conforme Decreto 683/25)
        if (fatura.getDocumentStatus() != null) {
            dto.setDocumentStatus(fatura.getDocumentStatus().getCodigo());
        } else {
            dto.setDocumentStatus(DocumentStatus.NORMAL.getCodigo());
        }
        
        // Motivo de cancelamento (se aplicável)
        if (fatura.getDocumentCancelReason() != null) {
            dto.setDocumentCancelReason(fatura.getDocumentCancelReason().getCodigo());
        }
        
        // Documento rejeitado (para correções)
        if (fatura.getRejectedDocumentNo() != null) {
            dto.setRejectedDocumentNo(fatura.getRejectedDocumentNo());
        }
        
        // Assinatura digital
        if (fatura.getJwsDocumentSignature() == null || fatura.getJwsDocumentSignature().isBlank()) {
            try {
                String customerTaxId = (fatura.getCliente() != null) ? fatura.getCliente().getNif() : "999999999";
                String sig = signatureService.signDocument(
                    fatura.getNumero(),
                    fatura.getDataEmissao() != null ? fatura.getDataEmissao().toString() : "",
                    fatura.getTotal(),
                    customerTaxId,
                    fatura.getHash()
                );
                fatura.setJwsDocumentSignature(sig);
            } catch (Exception e) {
                fatura.setJwsDocumentSignature(null);
            }
        }
        dto.setJwsDocumentSignature(fatura.getJwsDocumentSignature());
        
        // Dados do cliente
        dto.setCustomerCountry(fatura.getCustomerCountry() != null ? fatura.getCustomerCountry() : "AO");
        if (fatura.getCliente() != null) {
            String nif = fatura.getCliente().getNif();
            dto.setCustomerTaxID(nif != null && !nif.isEmpty() ? nif : "999999999");
        } else {
            dto.setCustomerTaxID("999999999"); // Consumidor final
        }
        
        // Dados da empresa emitente
        if (fatura.getCompanyName() != null && !fatura.getCompanyName().isBlank()) {
            dto.setCompanyName(fatura.getCompanyName());
        } else {
            dto.setCompanyName(emitterCompanyName);
        }
        dto.setEacCode(fatura.getEacCode());
        
        // Linhas do documento
        dto.setLines(mapLines(fatura.getItens()));
        
        // Totais
        dto.setDocumentTotals(mapDocumentTotals(fatura));
        
        // PaymentReceipt (para FR)
        if (fatura.getTipoDocumento() == TipoDocumento.FATURA_RECIBO) {
            dto.setPaymentReceipt(createPaymentReceipt(fatura));
        }
        
        // Retenções na fonte
        if (fatura.getTotalRetencao() != null && fatura.getTotalRetencao().compareTo(BigDecimal.ZERO) > 0) {
            dto.setWithholdingTaxList(createWithholdingTaxList(fatura));
        }
        
        return dto;
    }

    /**
     * Mapeia as linhas da fatura.
     */
    private List<LineDTO> mapLines(List<ItemFatura> itens) {
        List<LineDTO> lines = new ArrayList<>();
        int lineNumber = 1;
        
        for (ItemFatura item : itens) {
            LineDTO line = new LineDTO();
            line.setLineNumber(lineNumber++);
            
            // Código e descrição do produto
            if (item.getProduto() != null) {
                line.setProductCode(String.valueOf(item.getProduto().getId()));
            } else {
                line.setProductCode("SRV"); // Serviço genérico
            }
            line.setProductDescription(item.getDescricao());
            
            // Quantidade e unidade
            line.setQuantity(BigDecimal.valueOf(item.getQuantidade()));
            line.setUnitOfMeasure(item.getProduto() != null && item.getProduto().getUnidadeMedida() != null 
                ? item.getProduto().getUnidadeMedida().name() : "UN");
            
            // Preços
            line.setUnitPrice(item.getPrecoUnitario());
            line.setUnitPriceBase(item.getPrecoUnitario()); // Após descontos (se houver)
            
            // Valores
            line.setDebitAmount(item.getTotal());
            line.setSettlementAmount(BigDecimal.ZERO); // Descontos aplicados
            
            // Impostos
            line.setTaxes(mapTaxes(item));
            
            lines.add(line);
        }
        
        return lines;
    }

    /**
     * Mapeia os impostos de uma linha.
     */
    private List<TaxDTO> mapTaxes(ItemFatura item) {
        List<TaxDTO> taxes = new ArrayList<>();
        
        TaxDTO tax = new TaxDTO();
        tax.setTaxType("IVA");
        tax.setTaxCountryRegion("AO");
        
        BigDecimal taxPercentage = item.getPercentualIva();
        if (taxPercentage != null && taxPercentage.compareTo(BigDecimal.ZERO) > 0) {
            tax.setTaxCode("NOR"); // Normal
            tax.setTaxPercentage(taxPercentage);
        } else {
            tax.setTaxCode("ISE"); // Isento
            tax.setTaxPercentage(BigDecimal.ZERO);
            if (item.getCodigoIsencao() != null) {
                tax.setTaxExemptionCode(item.getCodigoIsencao());
            }
        }
        
        tax.setTaxContribution(item.getValorIva());
        taxes.add(tax);
        
        return taxes;
    }

    /**
     * Mapeia os totais do documento.
     */
    private DocumentTotalsDTO mapDocumentTotals(Fatura fatura) {
        DocumentTotalsDTO totals = new DocumentTotalsDTO();
        totals.setNetTotal(fatura.getSubtotal());
        totals.setTaxPayable(fatura.getIva());
        totals.setGrossTotal(fatura.getTotal());
        return totals;
    }

    /**
     * Cria o paymentReceipt para Fatura/Recibo (FR).
     */
    private PaymentReceiptDTO createPaymentReceipt(Fatura fatura) {
        PaymentReceiptDTO receipt = new PaymentReceiptDTO();
        
        List<PaymentReceiptDTO.SourceDocumentDTO> sourceDocuments = new ArrayList<>();
        PaymentReceiptDTO.SourceDocumentDTO sourceDoc = new PaymentReceiptDTO.SourceDocumentDTO();
        
        sourceDoc.setLineNo(1);
        sourceDoc.setCreditAmount(fatura.getTotal()); // Valor pago
        
        PaymentReceiptDTO.SourceDocumentIDDTO sourceDocId = new PaymentReceiptDTO.SourceDocumentIDDTO();
        sourceDocId.setOriginatingON(fatura.getNumero());
        sourceDocId.setDocumentDate(fatura.getDataEmissao().format(DATE_FORMATTER));
        
        sourceDoc.setSourceDocumentID(sourceDocId);
        sourceDocuments.add(sourceDoc);
        
        receipt.setSourceDocuments(sourceDocuments);
        return receipt;
    }

    /**
     * Cria a lista de retenções na fonte.
     */
    private List<WithholdingTaxDTO> createWithholdingTaxList(Fatura fatura) {
        List<WithholdingTaxDTO> list = new ArrayList<>();
        
        if (fatura.getTotalRetencao() != null && fatura.getTotalRetencao().compareTo(BigDecimal.ZERO) > 0) {
            WithholdingTaxDTO withholding = new WithholdingTaxDTO();
            withholding.setWithholdingTaxType("IR"); // Imposto de Renda
            withholding.setWithholdingTaxDescription("Retenção na Fonte - 6.5%");
            withholding.setWithholdingTaxAmount(fatura.getTotalRetencao());
            list.add(withholding);
        }
        
        return list;
    }

    /**
     * Converte TipoDocumento para código AGT.
     */
    private String getDocumentTypeCode(TipoDocumento tipo) {
        if (tipo == null) return "FT";
        
        return switch (tipo) {
            case FATURA -> "FT";
            case FATURA_RECIBO -> "FR";
            case RECIBO -> "RC";
            case NOTA_CREDITO -> "NC";
            case NOTA_DEBITO -> "ND";
            case GUIA_TRANSPORTE -> "GT";
            case GUIA_REMESSA -> "GR";
            default -> "FT";
        };
    }
}
