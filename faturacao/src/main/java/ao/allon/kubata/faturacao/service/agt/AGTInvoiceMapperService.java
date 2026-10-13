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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
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

    @Value("${agt.software.signature-version:1}")
    private Integer signatureVersion;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public AGTInvoiceMapperService(JWSDigitalSignatureService signatureService) {
        this.signatureService = signatureService;
    }

    /**
     * Cria a requisição de submissão completa para a API AGT.
     */
    public AGTInvoiceSubmissionRequest createSubmissionRequest(List<Fatura> faturas) {
        if (faturas == null || faturas.isEmpty() || faturas.size() > 30) {
            throw new IllegalArgumentException("A submissão AGT deve conter entre 1 e 30 documentos.");
        }
        String submissionUUID = UUID.randomUUID().toString();
        String timestamp = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        int signatureVersionValue = signatureVersion == null ? 1 : signatureVersion;
        AGTInvoiceSubmissionRequest.SoftwareInfoDetail softwareInfoDetail =
            new AGTInvoiceSubmissionRequest.SoftwareInfoDetail(productId, productVersion, softwareValidationNumber, signatureVersionValue);
        Map<String, Object> softwarePayload = new LinkedHashMap<>();
        softwarePayload.put("productId", productId);
        softwarePayload.put("productVersion", productVersion);
        softwarePayload.put("softwareValidationNumber", softwareValidationNumber);
        softwarePayload.put("signatureVersion", signatureVersionValue);
        String softwareSignature = signatureService.signJwsPayload(softwarePayload);
        AGTInvoiceSubmissionRequest.SoftwareInfo softwareInfo =
            new AGTInvoiceSubmissionRequest.SoftwareInfo(softwareInfoDetail, softwareSignature);

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
        dto.setSystemEntryDate(fatura.getSystemEntryDate()
            .atZone(ZoneId.systemDefault())
            .withZoneSameInstant(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        
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
        
        // A assinatura JWS é gerada depois de preencher todos os campos oficiais da AGT.
        
        // Dados do cliente
        dto.setCustomerCountry(fatura.getCustomerCountry() != null ? fatura.getCustomerCountry() : "AO");
        if (fatura.getCliente() != null) {
            String nif = fatura.getCliente().getNif();
            dto.setCustomerTaxID(nif != null && !nif.isEmpty() ? nif : "999999999");
        } else {
            dto.setCustomerTaxID("999999999"); // Consumidor final
        }
        
        // O contrato AGT usa companyName para a denominação do comprador.
        String buyerName = fatura.getCompanyName();
        if ((buyerName == null || buyerName.isBlank()) && fatura.getCliente() != null) {
            buyerName = fatura.getCliente().getNome();
        }
        if (buyerName == null || buyerName.isBlank()) {
            buyerName = "Consumidor Final";
        }
        dto.setCompanyName(buyerName);
        dto.setEacCode(fatura.getEacCode());
        dto.setLines(mapLines(fatura.getItens()));
        DocumentTotalsDTO totals = mapDocumentTotals(fatura);
        dto.setDocumentTotals(totals);
        Map<String, Object> signedPayload = new LinkedHashMap<>();
        signedPayload.put("documentNo", dto.getDocumentNo());
        signedPayload.put("taxRegistrationNumber", emitterNif);
        signedPayload.put("documentType", dto.getDocumentType());
        signedPayload.put("documentDate", dto.getDocumentDate());
        signedPayload.put("customerTaxID", dto.getCustomerTaxID());
        signedPayload.put("customerCountry", dto.getCustomerCountry());
        signedPayload.put("companyName", dto.getCompanyName());
        signedPayload.put("documentTotals", totals);
        String documentSignature = signatureService.signJwsPayload(signedPayload);
        dto.setJwsDocumentSignature(documentSignature);
        fatura.setJwsDocumentSignature(documentSignature);
        
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
            line.setOperationType(item.getProduto() != null ? "TB" : "SG");

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
            // Valor tributável da venda, sem IVA: a AGT espera creditAmount.
            line.setCreditAmount(item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO);
            line.setSettlementAmount(BigDecimal.ZERO); // Descontos ainda não parametrizados na linha
            
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
