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
            submissionUUID,
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
        if (fatura == null) {
            throw new IllegalArgumentException("O documento de facturação é obrigatório.");
        }
        if (fatura.getNumero() == null || fatura.getNumero().isBlank()
                || fatura.getNumero().length() < 8 || fatura.getNumero().length() > 60) {
            throw new IllegalArgumentException("O número fiscal do documento deve ter entre 8 e 60 caracteres.");
        }
        if (fatura.getDataEmissao() == null || fatura.getSystemEntryDate() == null) {
            throw new IllegalStateException("Data de emissão e data de gravação são obrigatórias para a AGT.");
        }
        boolean receiptType = fatura.getTipoDocumento() == TipoDocumento.RECIBO;
        if (!receiptType && (fatura.getItens() == null || fatura.getItens().isEmpty())) {
            throw new IllegalStateException("O documento deve conter linhas de artigos ou serviços para a AGT.");
        }
        Fatura sourceForReceipt = null;
        if (receiptType) {
            sourceForReceipt = resolveSourceDocument(fatura);
            if (fatura.getTotal() == null || sourceForReceipt.getTotal() == null
                    || fatura.getTotal().compareTo(sourceForReceipt.getTotal()) != 0
                    || fatura.getSubtotal() == null || sourceForReceipt.getSubtotal() == null
                    || fatura.getSubtotal().compareTo(sourceForReceipt.getSubtotal()) != 0
                    || fatura.getIva() == null || sourceForReceipt.getIva() == null
                    || fatura.getIva().compareTo(sourceForReceipt.getIva()) != 0) {
                throw new IllegalStateException(
                    "O recibo RC só pode ser submetido quando subtotal, IVA e total corresponderem integralmente ao documento de origem. "
                    + "Recibos parciais e reconciliações com valores divergentes ainda não são suportados.");
            }
        }
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
        String buyerName = fatura.getCliente() != null ? fatura.getCliente().getNome() : null;
        if (buyerName == null || buyerName.isBlank()) {
            buyerName = fatura.getCompanyName();
        }
        if (buyerName == null || buyerName.isBlank()) {
            buyerName = "Consumidor Final";
        }
        dto.setCompanyName(buyerName);
        dto.setEacCode(fatura.getEacCode());
        if (receiptType) {
            dto.setPaymentReceipt(createPaymentReceipt(fatura, sourceForReceipt));
        } else {
            dto.setLines(mapLines(fatura.getItens()));
        }
        if (fatura.getTipoDocumento() == TipoDocumento.NOTA_CREDITO
                || fatura.getTipoDocumento() == TipoDocumento.NOTA_DEBITO) {
            Fatura source = resolveSourceDocument(fatura);
            ReferenceInfoDTO referenceInfo = new ReferenceInfoDTO();
            referenceInfo.setReference(source.getNumero());
            String reason = fatura.getMotivoCancelamento();
            if (reason == null || reason.isBlank()) reason = fatura.getObservacoes();
            if (reason != null && !reason.isBlank()) {
                referenceInfo.setReason(reason.length() > 60 ? reason.substring(0, 60) : reason);
            }
            dto.setReferenceInfo(referenceInfo);
        }
        DocumentTotalsDTO totals = mapDocumentTotals(fatura);
        dto.setDocumentTotals(totals);
        Map<String, Object> signedTotals = new LinkedHashMap<>();
        signedTotals.put("taxPayable", totals.getTaxPayable());
        signedTotals.put("netTotal", totals.getNetTotal());
        signedTotals.put("grossTotal", totals.getGrossTotal());
        Map<String, Object> signedPayload = new LinkedHashMap<>();
        signedPayload.put("documentNo", dto.getDocumentNo());
        signedPayload.put("taxRegistrationNumber", emitterNif);
        signedPayload.put("documentType", dto.getDocumentType());
        signedPayload.put("documentDate", dto.getDocumentDate());
        signedPayload.put("customerTaxID", dto.getCustomerTaxID());
        signedPayload.put("customerCountry", dto.getCustomerCountry());
        signedPayload.put("companyName", dto.getCompanyName());
        signedPayload.put("documentTotals", signedTotals);
        String documentSignature = signatureService.signJwsPayload(signedPayload);
        dto.setJwsDocumentSignature(documentSignature);
        fatura.setJwsDocumentSignature(documentSignature);
        
        // No contrato AGT, paymentReceipt só é usado por AR/RG.
        // FR mantém as linhas de artigos/serviços e os respectivos totais.
        
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
                String productCode = item.getProduto().getCodigo();
                line.setProductCode(productCode != null && !productCode.isBlank()
                    ? productCode : String.valueOf(item.getProduto().getId()));
            } else {
                line.setProductCode("SRV");
            }
            line.setProductDescription(item.getDescricao());

            // Quantidade decimal é preservada na comunicação fiscal.
            BigDecimal quantity = item.getQuantidadeDecimal() != null
                ? item.getQuantidadeDecimal()
                : BigDecimal.valueOf(item.getQuantidade() == null ? 0 : item.getQuantidade());
            line.setQuantity(quantity);
            line.setUnitOfMeasure(mapUnitOfMeasure(item));

            // unitPriceBase é o preço sem desconto e sem imposto;
            // unitPrice é o preço líquido por unidade, sem imposto.
            BigDecimal unitPriceBase = item.getPrecoUnitario() == null
                ? BigDecimal.ZERO : item.getPrecoUnitario();
            BigDecimal netLine = item.getSubtotal() == null ? BigDecimal.ZERO : item.getSubtotal();
            BigDecimal unitPrice = quantity.signum() > 0
                ? netLine.divide(quantity, 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            BigDecimal grossBase = unitPriceBase.multiply(quantity);
            BigDecimal settlement = grossBase.subtract(netLine).max(BigDecimal.ZERO)
                .setScale(2, java.math.RoundingMode.HALF_UP);
            line.setUnitPriceBase(unitPriceBase);
            line.setUnitPrice(unitPrice);
            // Vendas: apenas creditAmount preenchido; impostos são enviados separadamente.
            line.setDebitAmount(BigDecimal.ZERO);
            line.setCreditAmount(netLine);
            line.setSettlementAmount(settlement);
            
            // Impostos
            line.setTaxes(mapTaxes(item));
            
            lines.add(line);
        }
        
        return lines;
    }

    private String mapUnitOfMeasure(ItemFatura item) {
        if (item.getProduto() == null || item.getProduto().getUnidadeMedida() == null) return "UN";
        return switch (item.getProduto().getUnidadeMedida()) {
            case UNIDADE -> "UN";
            case KILOGRAMA -> "KG";
            case LITRO -> "L";
            case METRO -> "M";
            case CAIXA -> "CX";
            case SERVICO -> "SV";
            case HORA -> "H";
        };
    }

    /**
     * Mapeia IVA para os códigos admitidos pela AGT. O regime de imposto do
     * produto prevalece quando contém um código fiscal explicitamente suportado.
     */
    private List<TaxDTO> mapTaxes(ItemFatura item) {
        List<TaxDTO> taxes = new ArrayList<>();
        BigDecimal taxPercentage = item.getPercentualIva() == null
            ? BigDecimal.ZERO : item.getPercentualIva();

        TaxDTO tax = new TaxDTO();
        tax.setTaxType("IVA");
        tax.setTaxCountryRegion("AO");

        String taxCode = null;
        if (taxPercentage.compareTo(BigDecimal.ZERO) > 0
                && item.getProduto() != null && item.getProduto().getImposto() != null
                && "IVA".equalsIgnoreCase(item.getProduto().getImposto().getTipo())) {
            String configuredCode = item.getProduto().getImposto().getCodigo();
            if (configuredCode != null && List.of("NOR", "INT", "RED", "OUT")
                    .contains(configuredCode.trim().toUpperCase(java.util.Locale.ROOT))) {
                taxCode = configuredCode.trim().toUpperCase(java.util.Locale.ROOT);
            }
        }

        if (taxPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            taxCode = "ISE";
            tax.setTaxPercentage(BigDecimal.ZERO);
            String exemptionCode = item.getCodigoIsencao();
            if (exemptionCode == null || exemptionCode.isBlank()) {
                throw new IllegalStateException("Item isento sem código de isenção AGT: " + item.getDescricao());
            }
            if (!exemptionCode.matches("[A-Z0-9]{3}")) {
                throw new IllegalStateException("Código de isenção AGT deve ter 3 caracteres: " + item.getDescricao());
            }
            tax.setTaxExemptionCode(exemptionCode);
        } else {
            if (taxCode == null) {
                if (taxPercentage.compareTo(new BigDecimal("14.00")) == 0) {
                    taxCode = "NOR";
                } else if (taxPercentage.compareTo(new BigDecimal("7.00")) == 0) {
                    taxCode = "RED";
                } else {
                    throw new IllegalStateException(
                        "Código fiscal AGT não configurado para IVA de " + taxPercentage
                        + "% no artigo/serviço " + item.getDescricao()
                        + ". Configure um código AGT explícito antes de submeter.");
                }
            }
            tax.setTaxCode(taxCode);
            tax.setTaxPercentage(taxPercentage);
        }

        if (taxPercentage.compareTo(BigDecimal.ZERO) > 0 && item.getValorIva() == null) {
            throw new IllegalStateException(
                "O IVA da linha não foi calculado: " + item.getDescricao());
        }
        BigDecimal taxContribution = item.getValorIva() == null ? BigDecimal.ZERO : item.getValorIva();
        tax.setTaxContribution(taxContribution.setScale(2, java.math.RoundingMode.CEILING));
        taxes.add(tax);
        return taxes;
    }

    /**
     * Mapeia os totais do documento.
     */
    private DocumentTotalsDTO mapDocumentTotals(Fatura fatura) {
        DocumentTotalsDTO totals = new DocumentTotalsDTO();
        totals.setNetTotal(fatura.getSubtotal() == null ? BigDecimal.ZERO : fatura.getSubtotal().setScale(2, java.math.RoundingMode.HALF_UP));
        totals.setTaxPayable(fatura.getIva() == null ? BigDecimal.ZERO : fatura.getIva().setScale(2, java.math.RoundingMode.HALF_UP));
        totals.setGrossTotal(fatura.getTotal() == null ? BigDecimal.ZERO : fatura.getTotal().setScale(2, java.math.RoundingMode.HALF_UP));
        return totals;
    }

    /**
     * Cria o paymentReceipt para Fatura/Recibo (FR).
     */
    private Fatura resolveSourceDocument(Fatura fatura) {
        Fatura source = fatura.getFaturaReferencia();
        if (source == null) source = fatura.getFaturaOriginal();
        if (source == null || source.getNumero() == null || source.getNumero().isBlank()
                || source.getDataEmissao() == null) {
            throw new IllegalStateException(
                "Este documento exige uma factura/documento de origem persistido e identificado.");
        }
        return source;
    }

    private PaymentReceiptDTO createPaymentReceipt(Fatura recibo, Fatura source) {
        if (source == null) {
            throw new IllegalStateException("O recibo AGT necessita de um documento de origem.");
        }
        BigDecimal amountPaid = recibo.getTotal();
        if (amountPaid == null || amountPaid.signum() <= 0) {
            throw new IllegalStateException("O valor do recibo deve ser positivo.");
        }

        PaymentReceiptDTO receipt = new PaymentReceiptDTO();
        List<PaymentReceiptDTO.SourceDocumentDTO> sourceDocuments = new ArrayList<>();
        PaymentReceiptDTO.SourceDocumentDTO sourceDoc = new PaymentReceiptDTO.SourceDocumentDTO();
        sourceDoc.setLineNo(1);
        sourceDoc.setCreditAmount(amountPaid);

        PaymentReceiptDTO.SourceDocumentIDDTO sourceDocId = new PaymentReceiptDTO.SourceDocumentIDDTO();
        sourceDocId.setOriginatingON(source.getNumero());
        sourceDocId.setDocumentDate(source.getDataEmissao().format(DATE_FORMATTER));
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
            withholding.setWithholdingTaxType("IRT");
            withholding.setWithholdingTaxDescription("Retenção na fonte");
            withholding.setWithholdingTaxAmount(fatura.getTotalRetencao());
            list.add(withholding);
        }
        
        return list;
    }

    /**
     * Converte TipoDocumento para código AGT.
     */
    private String getDocumentTypeCode(TipoDocumento tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("O tipo de documento é obrigatório para submissão à AGT.");
        }
        return switch (tipo) {
            case FATURA -> "FT";
            case FATURA_RECIBO -> "FR";
            case VENDA_A_DINHEIRO -> "TV";
            case RECIBO -> "RC";
            case NOTA_CREDITO -> "NC";
            case NOTA_DEBITO -> "ND";
            case AUTO_FATURA -> "AF";
            default -> throw new IllegalArgumentException(
                "O tipo de documento " + tipo + " não é suportado no serviço de facturação electrónica da AGT.");
        };
    }
}
