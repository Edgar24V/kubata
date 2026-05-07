package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.*;
import ao.allon.kubata.faturacao.domain.enums.MetodoPagamento;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.repository.ProdutoRepository;
import ao.allon.kubata.faturacao.repository.ClienteRepository;
import ao.allon.kubata.faturacao.repository.ImpostoRepository;
import ao.allon.kubata.faturacao.repository.ReciboRepository;
import ao.allon.kubata.faturacao.repository.MotivoIsencaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SaftAoExportService {

    private final EmpresaService empresaService;
    private final FaturaRepository faturaRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final ImpostoRepository impostoRepository;
    private final ReciboRepository reciboRepository;
    private final MotivoIsencaoRepository motivoIsencaoRepository;
    private final SaftValidatorService saftValidatorService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public SaftAoExportService(EmpresaService empresaService,
                               FaturaRepository faturaRepository,
                               ProdutoRepository produtoRepository,
                               ClienteRepository clienteRepository,
                               ImpostoRepository impostoRepository,
                               ReciboRepository reciboRepository,
                               MotivoIsencaoRepository motivoIsencaoRepository,
                               SaftValidatorService saftValidatorService) {
        this.empresaService = empresaService;
        this.faturaRepository = faturaRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.impostoRepository = impostoRepository;
        this.reciboRepository = reciboRepository;
        this.motivoIsencaoRepository = motivoIsencaoRepository;
        this.saftValidatorService = saftValidatorService;
    }

    @Transactional(readOnly = true)
    public void exportProdutosToSaftAo(File file) throws Exception {
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.withDayOfMonth(1);
        LocalDate endDate = now.withDayOfMonth(now.lengthOfMonth());
        exportFullSaft(file, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public void exportFullSaft(File file, LocalDate startDate, LocalDate endDate) throws Exception {
        // Validar antes de exportar
        List<String> validationErrors = saftValidatorService.validateForExport(startDate, endDate);
        if (!validationErrors.isEmpty()) {
            throw new IllegalStateException("Erros de validação SAF-T encontrados:\n" + String.join("\n", validationErrors));
        }

        Empresa empresa = empresaService.getDadosEmpresa();
        
        // Fetch data
        List<Fatura> allFaturas = faturaRepository.findByDataEmissaoBetween(startDate, endDate);
        List<Produto> produtos = produtoRepository.findAll();
        List<Cliente> clientes = clienteRepository.findAll();
        List<Imposto> impostos = impostoRepository.findAll();
        List<Recibo> recibos = reciboRepository.findByDataRecebimentoBetween(startDate, endDate);

        // Separate documents by type
        List<Fatura> salesInvoices = allFaturas.stream()
            .filter(f -> isSalesInvoice(f.getSerie().getTipoDocumento()))
            .collect(Collectors.toList());

        List<Fatura> workingDocuments = allFaturas.stream()
            .filter(f -> isWorkingDocument(f.getSerie().getTipoDocumento()))
            .collect(Collectors.toList());

        List<Fatura> movementOfGoods = allFaturas.stream()
            .filter(f -> isMovementOfGoods(f.getSerie().getTipoDocumento()))
            .collect(Collectors.toList());

        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            out.println("<AuditFile xmlns=\"urn:OECD:StandardAuditFile-Tax:AO_1.01_01\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
            
            writeHeader(out, empresa, startDate, endDate);
            writeMasterFiles(out, produtos, clientes, impostos);
            writeSourceDocuments(out, salesInvoices, movementOfGoods, workingDocuments, produtos, impostos, recibos);
            
            out.println("</AuditFile>");
        }

        // Calcular Hash SHA-256 do arquivo gerado para auditoria
        String fileHash = calculateSha256(file);
        // Logar auditoria (Simulado aqui, idealmente persistir em tabela de logs)
        System.out.println("SAF-T Exportado: " + file.getName() + " | SHA-256: " + fileHash + " | User: " + System.getProperty("user.name") + " | Time: " + LocalDateTime.now());
    }

    private String calculateSha256(File file) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        try (java.io.InputStream fis = new java.io.FileInputStream(file)) {
            byte[] byteArray = new byte[1024];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private boolean isSalesInvoice(TipoDocumento tipo) {
        return tipo == TipoDocumento.FATURA || tipo == TipoDocumento.FATURA_RECIBO || 
               tipo == TipoDocumento.NOTA_CREDITO || tipo == TipoDocumento.NOTA_DEBITO;
    }

    private boolean isWorkingDocument(TipoDocumento tipo) {
        return tipo == TipoDocumento.PRO_FORMA || tipo == TipoDocumento.ORCAMENTO;
    }

    private boolean isMovementOfGoods(TipoDocumento tipo) {
        return tipo == TipoDocumento.GUIA_REMESSA || tipo == TipoDocumento.GUIA_TRANSPORTE;
    }

    private void writeHeader(PrintWriter out, Empresa empresa, LocalDate startDate, LocalDate endDate) {
        out.println("  <Header>");
        out.println("    <AuditFileVersion>1.01_01</AuditFileVersion>");
        out.println("    <CompanyID>" + escape(empresa.getNif()) + "</CompanyID>");
        out.println("    <TaxRegistrationNumber>" + escape(empresa.getNif()) + "</TaxRegistrationNumber>");
        out.println("    <TaxAccountingBasis>F</TaxAccountingBasis>"); // F = Faturação
        out.println("    <CompanyName>" + escape(empresa.getNome()) + "</CompanyName>");
        
        // Endereço Empresa
        out.println("    <CompanyAddress>");
        out.println("      <AddressDetail>" + escape(empresa.getEndereco()) + "</AddressDetail>");
        out.println("      <City>" + escape(empresa.getCidade()) + "</City>");
        out.println("      <Country>AO</Country>");
        out.println("    </CompanyAddress>");
        
        out.println("    <FiscalYear>" + startDate.getYear() + "</FiscalYear>");
        out.println("    <StartDate>" + startDate.format(DATE_FMT) + "</StartDate>");
        out.println("    <EndDate>" + endDate.format(DATE_FMT) + "</EndDate>");
        out.println("    <CurrencyCode>AOA</CurrencyCode>");
        out.println("    <DateCreated>" + LocalDate.now().format(DATE_FMT) + "</DateCreated>");
        out.println("    <TaxEntity>Global</TaxEntity>");
        out.println("    <ProductCompanyTaxID>" + escape(empresa.getNif()) + "</ProductCompanyTaxID>");
        
        String softVal = empresa.getSoftwareValidationNumber();
        if (softVal == null || softVal.isBlank() || softVal.contains("0000")) {
            softVal = "0000/AGT/2024"; // Fallback para desenvolvimento
        }
        
        out.println("    <SoftwareValidationNumber>" + escape(softVal) + "</SoftwareValidationNumber>");
        out.println("    <ProductID>Kubata/Faturacao</ProductID>");
        out.println("    <ProductVersion>1.0.0</ProductVersion>");
        out.println("  </Header>");
    }

    private void writeMasterFiles(PrintWriter out, List<Produto> produtos, List<Cliente> clientes, List<Imposto> impostos) {
        out.println("  <MasterFiles>");
        
        // 2.1 Customer
        for (Cliente c : clientes) {
            out.println("    <Customer>");
            out.println("      <CustomerID>" + c.getId() + "</CustomerID>");
            out.println("      <AccountID>Unknown</AccountID>"); // Conta Contabilistica
            out.println("      <CustomerTaxID>" + (c.getNif() != null ? escape(c.getNif()) : "999999999") + "</CustomerTaxID>");
            out.println("      <CompanyName>" + escape(c.getNome()) + "</CompanyName>");
            out.println("      <BillingAddress>");
            out.println("        <AddressDetail>" + (c.getEndereco() != null ? escape(c.getEndereco()) : "Desconhecido") + "</AddressDetail>");
            out.println("        <City>" + (c.getCidade() != null ? escape(c.getCidade()) : "Desconhecido") + "</City>");
            out.println("        <Country>AO</Country>");
            out.println("      </BillingAddress>");
            out.println("      <SelfBillingIndicator>0</SelfBillingIndicator>");
            out.println("    </Customer>");
        }

        // 2.4 Product
        for (Produto p : produtos) {
            out.println("    <Product>");
            out.println("      <ProductType>" + (p.isServico() ? "S" : "P") + "</ProductType>");
            out.println("      <ProductCode>" + p.getCodigo() + "</ProductCode>");
            out.println("      <ProductGroup>Geral</ProductGroup>");
            out.println("      <ProductDescription>" + escape(p.getNome()) + "</ProductDescription>");
            out.println("      <ProductNumberCode>" + p.getCodigo() + "</ProductNumberCode>");
            out.println("    </Product>");
        }

        // 2.5 TaxTable
        out.println("    <TaxTable>");
        for (Imposto i : impostos) {
            out.println("      <TaxTableEntry>");
            out.println("        <TaxType>" + i.getTipo() + "</TaxType>"); // IVA, NS, etc
            out.println("        <TaxCountryRegion>AO</TaxCountryRegion>");
            out.println("        <TaxCode>" + i.getCodigo() + "</TaxCode>");
            out.println("        <Description>" + escape(i.getDescricao()) + "</Description>");
            out.println("        <TaxPercentage>" + i.getPercentual() + "</TaxPercentage>");
            out.println("      </TaxTableEntry>");
        }
        out.println("    </TaxTable>");
        
        out.println("  </MasterFiles>");
    }

    private void writeSourceDocuments(PrintWriter out, List<Fatura> salesInvoices, List<Fatura> movementOfGoods, List<Fatura> workingDocuments, List<Produto> produtos, List<Imposto> impostos, List<Recibo> recibos) {
        out.println("  <SourceDocuments>");
        
        // 4.1 SalesInvoices
        if (!salesInvoices.isEmpty()) {
            out.println("    <SalesInvoices>");
            out.println("      <NumberOfEntries>" + salesInvoices.size() + "</NumberOfEntries>");
            
            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;
            
            for (Fatura f : salesInvoices) {
                if (f.getSerie().getTipoDocumento() == TipoDocumento.NOTA_CREDITO) {
                    totalCredit = totalCredit.add(f.getTotal());
                } else {
                    totalDebit = totalDebit.add(f.getTotal());
                }
            }
            
            out.println("      <TotalDebit>" + totalDebit + "</TotalDebit>");
            out.println("      <TotalCredit>" + totalCredit + "</TotalCredit>");

            for (Fatura f : salesInvoices) {
                writeInvoice(out, f, produtos, impostos);
            }
            out.println("    </SalesInvoices>");
        }

        // 4.2 MovementOfGoods
        if (!movementOfGoods.isEmpty()) {
            out.println("    <MovementOfGoods>");
            out.println("      <NumberOfMovementLines>" + movementOfGoods.stream().mapToLong(f -> f.getItens().size()).sum() + "</NumberOfMovementLines>");
            out.println("      <TotalQuantityIssued>" + movementOfGoods.stream().flatMap(f -> f.getItens().stream()).mapToInt(ItemFatura::getQuantidade).sum() + "</TotalQuantityIssued>");
            for (Fatura f : movementOfGoods) {
                writeStockMovement(out, f, produtos, impostos);
            }
            out.println("    </MovementOfGoods>");
        }

        // 4.3 WorkingDocuments
        if (!workingDocuments.isEmpty()) {
            out.println("    <WorkingDocuments>");
            out.println("      <NumberOfEntries>" + workingDocuments.size() + "</NumberOfEntries>");
             BigDecimal totalDebit = workingDocuments.stream().map(Fatura::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            out.println("      <TotalDebit>" + totalDebit + "</TotalDebit>");
            for (Fatura f : workingDocuments) {
                writeWorkDocument(out, f, produtos, impostos);
            }
            out.println("    </WorkingDocuments>");
        }

        // 4.4 Payments
        if (!recibos.isEmpty()) {
            writePayments(out, recibos);
        }

        out.println("  </SourceDocuments>");
    }

    private void writePayments(PrintWriter out, List<Recibo> recibos) {
        out.println("    <Payments>");
        out.println("      <NumberOfEntries>" + recibos.size() + "</NumberOfEntries>");
        
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = recibos.stream().map(Recibo::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        
        out.println("      <TotalDebit>" + totalDebit + "</TotalDebit>");
        out.println("      <TotalCredit>" + totalCredit + "</TotalCredit>");
        
        for (Recibo r : recibos) {
            out.println("      <Payment>");
            out.println("        <PaymentRefNo>" + escape(r.getNumero()) + "</PaymentRefNo>");
            out.println("        <Period>" + r.getDataRecebimento().getMonthValue() + "</Period>");
            out.println("        <TransactionDate>" + r.getDataRecebimento().format(DATE_FMT) + "</TransactionDate>");
            out.println("        <PaymentType>RG</PaymentType>"); // RG = Recibo Geral
            out.println("        <Description>" + escape(r.getObservacoes() != null ? r.getObservacoes() : "Pagamento de Fatura") + "</Description>");
            out.println("        <SystemID>" + r.getId() + "</SystemID>");
            
            out.println("        <DocumentStatus>");
            out.println("          <PaymentStatus>N</PaymentStatus>"); // N = Normal
            out.println("          <PaymentStatusDate>" + r.getDataRecebimento().atStartOfDay().format(DATE_TIME_FMT) + "</PaymentStatusDate>");
            out.println("          <SourceID>" + "Admin" + "</SourceID>"); // TODO: Use actual user
            out.println("          <SourcePayment>P</SourcePayment>");
            out.println("        </DocumentStatus>");
            
            out.println("        <PaymentMethod>");
            out.println("          <PaymentMechanism>" + getPaymentMechanism(r.getMetodoPagamento()) + "</PaymentMechanism>");
            out.println("          <PaymentAmount>" + r.getValor() + "</PaymentAmount>");
            out.println("          <PaymentDate>" + r.getDataRecebimento().format(DATE_FMT) + "</PaymentDate>");
            out.println("        </PaymentMethod>");
            
            out.println("        <SourceDocumentID>");
            if (r.getFatura() != null) {
                out.println("          <OriginatingON>" + escape(r.getFatura().getNumero()) + "</OriginatingON>");
                out.println("          <InvoiceDate>" + r.getFatura().getDataEmissao().format(DATE_FMT) + "</InvoiceDate>");
                out.println("          <Description>" + "Pagamento referente a fatura " + r.getFatura().getNumero() + "</Description>");
            } else {
                out.println("          <OriginatingON>Desconhecido</OriginatingON>");
                out.println("          <InvoiceDate>" + r.getDataRecebimento().format(DATE_FMT) + "</InvoiceDate>");
                out.println("          <Description>Pagamento sem fatura associada</Description>");
            }
            out.println("        </SourceDocumentID>");
            
            out.println("      </Payment>");
        }
        
        out.println("    </Payments>");
    }

    private String getPaymentMechanism(MetodoPagamento metodo) {
        if (metodo == null) return "NU";
        switch (metodo) {
            case DINHEIRO: return "NU";
            case TRANSFERENCIA_BANCARIA: return "TB";
            case CARTAO_POS: return "CD";
            case CHEQUE: return "CH";
            case MOBILE_MONEY: return "OU";
            default: return "OU";
        }
    }

    private void writeInvoice(PrintWriter out, Fatura f, List<Produto> produtos, List<Imposto> impostos) {
        out.println("      <Invoice>");
        out.println("        <InvoiceNo>" + escape(f.getNumero()) + "</InvoiceNo>");
        
        out.println("        <DocumentStatus>");
        out.println("          <InvoiceStatus>" + (f.getStatus() == StatusFatura.CANCELADA ? "A" : "N") + "</InvoiceStatus>");
        
        LocalDateTime statusDate = (f.getStatus() == StatusFatura.CANCELADA && f.getDataCancelamento() != null) 
            ? f.getDataCancelamento() 
            : getSystemEntryDate(f);
            
        out.println("          <InvoiceStatusDate>" + statusDate.format(DATE_TIME_FMT) + "</InvoiceStatusDate>");
        
        if (f.getStatus() == StatusFatura.CANCELADA) {
            String reason = f.getMotivoCancelamento();
            if (reason == null || reason.isBlank()) {
                reason = "Cancelamento de documento";
            }
            out.println("          <Reason>" + escape(reason) + "</Reason>");
        }
        
        out.println("          <SourceID>" + getSourceID(f) + "</SourceID>");
        out.println("          <SourceBilling>" + "P" + "</SourceBilling>");
        out.println("        </DocumentStatus>");
        
        out.println("        <Hash>" + f.getHash() + "</Hash>");
        out.println("        <HashControl>" + f.getHashControl() + "</HashControl>");
        
        // Campo 4.1.4.8 - Referência a documento anterior (obrigatório para NC de ano anterior ou retificações)
        if (f.getFaturaReferencia() != null) {
            out.println("        <SourceDocumentID>");
            out.println("          <OriginatingON>" + escape(f.getFaturaReferencia().getNumero()) + "</OriginatingON>");
            out.println("          <InvoiceDate>" + f.getFaturaReferencia().getDataEmissao().format(DATE_FMT) + "</InvoiceDate>");
            out.println("          <Description>" + escape(f.getMotivoCancelamento() != null ? f.getMotivoCancelamento() : "Retificação") + "</Description>");
            out.println("        </SourceDocumentID>");
        }
        
        out.println("        <Period>" + f.getDataEmissao().getMonthValue() + "</Period>");
        out.println("        <InvoiceDate>" + f.getDataEmissao().format(DATE_FMT) + "</InvoiceDate>");
        out.println("        <InvoiceType>" + f.getSerie().getTipoDocumento().getCodigo() + "</InvoiceType>");
        
        out.println("        <SpecialRegimes>");
        out.println("          <SelfBillingIndicator>0</SelfBillingIndicator>");
        out.println("          <CashVATSchemeIndicator>0</CashVATSchemeIndicator>");
        out.println("          <ThirdPartiesBillingIndicator>0</ThirdPartiesBillingIndicator>");
        out.println("        </SpecialRegimes>");
        
        out.println("        <SourceID>" + getSourceID(f) + "</SourceID>");
        out.println("        <SystemEntryDate>" + getSystemEntryDate(f).format(DATE_TIME_FMT) + "</SystemEntryDate>");
        out.println("        <CustomerID>" + f.getCliente().getId() + "</CustomerID>");
        
        // Lines
        int lineNum = 1;
        for (ItemFatura item : f.getItens()) {
            out.println("        <Line>");
            out.println("          <LineNumber>" + lineNum++ + "</LineNumber>");
            out.println("          <ProductCode>" + (item.getProduto() != null ? item.getProduto().getCodigo() : "GEN") + "</ProductCode>");
            out.println("          <ProductDescription>" + escape(item.getDescricao()) + "</ProductDescription>");
            out.println("          <Quantity>" + item.getQuantidade() + "</Quantity>");
            out.println("          <UnitOfMeasure>UN</UnitOfMeasure>");
            out.println("          <UnitPrice>" + item.getPrecoUnitario() + "</UnitPrice>");
            out.println("          <TaxPointDate>" + f.getDataEmissao().format(DATE_FMT) + "</TaxPointDate>");
            out.println("          <Description>" + escape(item.getDescricao()) + "</Description>");
            out.println("          <CreditAmount>" + item.getTotal() + "</CreditAmount>");
            
            // Tax
            out.println("          <Tax>");
            out.println("            <TaxType>IVA</TaxType>");
            out.println("            <TaxCountryRegion>AO</TaxCountryRegion>");
            out.println("            <TaxCode>" + getTaxCode(item, produtos, impostos) + "</TaxCode>");
            out.println("            <TaxPercentage>" + item.getPercentualIva() + "</TaxPercentage>");
            out.println("          </Tax>");
            
            // Exemption
            if (item.getPercentualIva().compareTo(BigDecimal.ZERO) == 0) {
                 String code = item.getCodigoIsencao();
                 String reason = item.getMotivoIsencao();
                 
                 // Tenta validar/buscar motivo real do banco se o código existir
                 if (code != null && !code.isEmpty()) {
                     MotivoIsencao motivo = motivoIsencaoRepository.findByCodigo(code).orElse(null);
                     if (motivo != null) {
                         reason = motivo.getDescricao(); // Use official description
                     }
                 }
                 
                 out.println("          <TaxExemptionReason>" + escape(reason != null ? reason : "Isento") + "</TaxExemptionReason>");
                 out.println("          <TaxExemptionCode>" + (code != null ? code : "M00") + "</TaxExemptionCode>");
            }
            
            out.println("          <SettlementAmount>0.00</SettlementAmount>");
            out.println("        </Line>");
        }
        out.println("      </Invoice>");
    }

    private LocalDateTime getSystemEntryDate(Fatura f) {
        // Fallback to createdAt if systemEntryDate is not available via public getter (assuming it might be missing in domain)
        // Ideally should use f.getSystemEntryDate()
        return f.getCreatedAt() != null ? f.getCreatedAt() : LocalDateTime.now();
    }

    private String getSourceID(Fatura f) {
        if (f.getUsuario() != null && f.getUsuario().getUsername() != null) {
            return f.getUsuario().getUsername();
        }
        return "Admin"; 
    }

    private void writeStockMovement(PrintWriter out, Fatura f, List<Produto> produtos, List<Imposto> impostos) {
        out.println("      <StockMovement>");
        out.println("        <DocumentNumber>" + escape(f.getNumero()) + "</DocumentNumber>");
        out.println("        <DocumentStatus>");
        out.println("          <MovementStatus>N</MovementStatus>");
        out.println("          <MovementStatusDate>" + f.getDataEmissao() + "T" + (f.getHoraEmissao() != null ? f.getHoraEmissao() : "00:00:00") + "</MovementStatusDate>");
        out.println("          <Reason>Emissao</Reason>");
        out.println("          <SourceID>" + getSourceID(f) + "</SourceID>");
        out.println("          <SourceBilling>P</SourceBilling>");
        out.println("        </DocumentStatus>");
        out.println("        <Hash>" + f.getHash() + "</Hash>");
        out.println("        <HashControl>1</HashControl>");
        out.println("        <Period>" + f.getDataEmissao().getMonthValue() + "</Period>");
        out.println("        <MovementDate>" + f.getDataEmissao() + "</MovementDate>");
        out.println("        <MovementType>" + f.getSerie().getTipoDocumento().getCodigo() + "</MovementType>");
        out.println("        <SystemEntryDate>" + getSystemEntryDate(f).format(DATE_TIME_FMT) + "</SystemEntryDate>");
        out.println("        <CustomerID>" + f.getCliente().getId() + "</CustomerID>");
        out.println("        <SourceID>" + getSourceID(f) + "</SourceID>");
        
        // Lines
        int lineNo = 1;
        for (ItemFatura item : f.getItens()) {
            out.println("        <Line>");
            out.println("          <LineNumber>" + lineNo++ + "</LineNumber>");
            out.println("          <ProductCode>" + (item.getProduto() != null ? item.getProduto().getCodigo() : "S001") + "</ProductCode>");
            out.println("          <ProductDescription>" + escape(item.getDescricao()) + "</ProductDescription>");
            out.println("          <Quantity>" + item.getQuantidade() + "</Quantity>");
            out.println("          <UnitOfMeasure>Un</UnitOfMeasure>");
            out.println("          <UnitPrice>" + item.getPrecoUnitario() + "</UnitPrice>");
            out.println("          <Description>" + escape(item.getDescricao()) + "</Description>");
            out.println("          <CreditAmount>" + item.getSubtotal() + "</CreditAmount>");
            
            out.println("          <Tax>");
            out.println("            <TaxType>IVA</TaxType>");
            out.println("            <TaxCountryRegion>AO</TaxCountryRegion>");
            out.println("            <TaxCode>" + getTaxCode(item, produtos, impostos) + "</TaxCode>");
            out.println("            <TaxPercentage>" + item.getPercentualIva() + "</TaxPercentage>");
            out.println("          </Tax>");
             
            // Exemption
            if (item.getPercentualIva().compareTo(BigDecimal.ZERO) == 0) {
                 String reason = item.getMotivoIsencao();
                 out.println("          <TaxExemptionReason>" + escape(reason != null ? reason : "Isento") + "</TaxExemptionReason>");
                 String code = item.getCodigoIsencao();
                 out.println("          <TaxExemptionCode>" + (code != null ? code : "M00") + "</TaxExemptionCode>");
            }

            out.println("          <SettlementAmount>0.00</SettlementAmount>");
            out.println("        </Line>");
        }
        out.println("        <DocumentTotals>");
        out.println("          <TaxPayable>" + f.getTotalImposto() + "</TaxPayable>");
        out.println("          <NetTotal>" + f.getSubtotal() + "</NetTotal>");
        out.println("          <GrossTotal>" + f.getTotal() + "</GrossTotal>");
        out.println("        </DocumentTotals>");
        out.println("      </StockMovement>");
    }

    private void writeWorkDocument(PrintWriter out, Fatura f, List<Produto> produtos, List<Imposto> impostos) {
        out.println("      <WorkDocument>");
        out.println("        <DocumentNumber>" + escape(f.getNumero()) + "</DocumentNumber>");
        out.println("        <DocumentStatus>");
        out.println("          <WorkStatus>N</WorkStatus>");
        out.println("          <WorkStatusDate>" + f.getDataEmissao() + "T" + (f.getHoraEmissao() != null ? f.getHoraEmissao() : "00:00:00") + "</WorkStatusDate>");
        out.println("          <Reason>Emissao</Reason>");
        out.println("          <SourceID>" + getSourceID(f) + "</SourceID>");
        out.println("          <SourceBilling>P</SourceBilling>");
        out.println("        </DocumentStatus>");
        out.println("        <Hash>" + f.getHash() + "</Hash>");
        out.println("        <HashControl>1</HashControl>");
        out.println("        <Period>" + f.getDataEmissao().getMonthValue() + "</Period>");
        out.println("        <WorkDate>" + f.getDataEmissao() + "</WorkDate>");
        out.println("        <WorkType>" + f.getSerie().getTipoDocumento().getCodigo() + "</WorkType>");
        out.println("        <SystemEntryDate>" + getSystemEntryDate(f).format(DATE_TIME_FMT) + "</SystemEntryDate>");
        out.println("        <CustomerID>" + f.getCliente().getId() + "</CustomerID>");
        
        // Lines
        int lineNo = 1;
        for (ItemFatura item : f.getItens()) {
            out.println("        <Line>");
            out.println("          <LineNumber>" + lineNo++ + "</LineNumber>");
            out.println("          <ProductCode>" + (item.getProduto() != null ? item.getProduto().getCodigo() : "S001") + "</ProductCode>");
            out.println("          <ProductDescription>" + escape(item.getDescricao()) + "</ProductDescription>");
            out.println("          <Quantity>" + item.getQuantidade() + "</Quantity>");
            out.println("          <UnitOfMeasure>Un</UnitOfMeasure>");
            out.println("          <UnitPrice>" + item.getPrecoUnitario() + "</UnitPrice>");
            out.println("          <Description>" + escape(item.getDescricao()) + "</Description>");
            out.println("          <CreditAmount>" + item.getSubtotal() + "</CreditAmount>");
            
            out.println("          <Tax>");
            out.println("            <TaxType>IVA</TaxType>");
            out.println("            <TaxCountryRegion>AO</TaxCountryRegion>");
            out.println("            <TaxCode>" + getTaxCode(item, produtos, impostos) + "</TaxCode>");
            out.println("            <TaxPercentage>" + item.getPercentualIva() + "</TaxPercentage>");
            out.println("          </Tax>");
            
            // Exemption
            if (item.getPercentualIva().compareTo(BigDecimal.ZERO) == 0) {
                 String reason = item.getMotivoIsencao();
                 out.println("          <TaxExemptionReason>" + escape(reason != null ? reason : "Isento") + "</TaxExemptionReason>");
                 String code = item.getCodigoIsencao();
                 out.println("          <TaxExemptionCode>" + (code != null ? code : "M00") + "</TaxExemptionCode>");
            }
            
            out.println("          <SettlementAmount>0.00</SettlementAmount>");
            out.println("        </Line>");
        }
        out.println("        <DocumentTotals>");
        out.println("          <TaxPayable>" + f.getTotalImposto() + "</TaxPayable>");
        out.println("          <NetTotal>" + f.getSubtotal() + "</NetTotal>");
        out.println("          <GrossTotal>" + f.getTotal() + "</GrossTotal>");
        out.println("        </DocumentTotals>");
        out.println("      </WorkDocument>");
    }

    private String getTaxCode(ItemFatura item, List<Produto> produtos, List<Imposto> impostos) {
        if (item.getPercentualIva().compareTo(BigDecimal.ZERO) == 0) {
            return "ISE"; // Isento
        }
        
        // Tentar encontrar o código pelo imposto do produto
        if (item.getProduto() != null && item.getProduto().getImposto() != null) {
            // Verifica se a taxa do produto ainda corresponde a taxa do item
            if (item.getProduto().getImposto().getPercentual().compareTo(item.getPercentualIva()) == 0) {
                return item.getProduto().getImposto().getCodigo();
            }
        }
        
        // Se não encontrar, procurar na lista de impostos por percentual
        for (Imposto imp : impostos) {
            if (imp.getPercentual().compareTo(item.getPercentualIva()) == 0) {
                return imp.getCodigo();
            }
        }
        
        return "NOR"; // Default para taxa normal se não encontrar
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
