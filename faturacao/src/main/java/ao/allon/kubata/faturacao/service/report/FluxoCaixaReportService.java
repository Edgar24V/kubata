package ao.allon.kubata.faturacao.service.report;

import ao.allon.kubata.faturacao.service.EmpresaService;
import ao.allon.kubata.faturacao.service.ReciboService;
import ao.allon.kubata.faturacao.service.DespesaService;
import ao.allon.kubata.faturacao.domain.Empresa;
import ao.allon.kubata.faturacao.domain.Recibo;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.design.JRDesignConditionalStyle;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.VerticalTextAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class FluxoCaixaReportService {

    private final EmpresaService empresaService;
    private final ReciboService reciboService;
    private final DespesaService despesaService;

    public FluxoCaixaReportService(EmpresaService empresaService, ReciboService reciboService, DespesaService despesaService) {
        this.empresaService = empresaService;
        this.reciboService = reciboService;
        this.despesaService = despesaService;
    }

    public JasperPrint prepararFluxoCaixa(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new IllegalArgumentException("Período inválido para fluxo de caixa.");
        }

        Map<String, Object> params = getEmpresaParams();
        params.put("PERIODO_INICIO", inicio);
        params.put("PERIODO_FIM", fim);

        List<RowFluxoCaixa> rows = buildRows(inicio, fim);

        try {
            JasperDesign design = new JasperDesign();
            design.setName("FluxoCaixa");
            design.setPageWidth(595);
            design.setPageHeight(842);
            design.setColumnWidth(515);
            design.setLeftMargin(40);
            design.setRightMargin(40);
            design.setTopMargin(40);
            design.setBottomMargin(40);

            JRDesignStyle normal = new JRDesignStyle();
            normal.setName("Normal");
            normal.setDefault(true);
            normal.setFontName("SansSerif");
            normal.setFontSize(10f);
            design.addStyle(normal);

            JRDesignConditionalStyle negativeStyle = new JRDesignConditionalStyle();
            negativeStyle.setBackcolor(new Color(255, 230, 230));
            negativeStyle.setMode(ModeEnum.OPAQUE);
            JRDesignExpression negativeExpr = new JRDesignExpression();
            negativeExpr.setText("$F{saldoDia}.compareTo(java.math.BigDecimal.ZERO) < 0");
            negativeStyle.setConditionExpression(negativeExpr);
            normal.addConditionalStyle(negativeStyle);

            JRDesignStyle titleStyle = new JRDesignStyle();
            titleStyle.setName("Title");
            titleStyle.setFontName("SansSerif");
            titleStyle.setBold(true);
            titleStyle.setFontSize(14f);
            design.addStyle(titleStyle);

            JRDesignStyle headerStyle = new JRDesignStyle();
            headerStyle.setName("Header");
            headerStyle.setFontName("SansSerif");
            headerStyle.setBold(true);
            headerStyle.setFontSize(10f);
            headerStyle.setBackcolor(new Color(240, 240, 240));
            headerStyle.setMode(ModeEnum.OPAQUE);
            design.addStyle(headerStyle);

            String[] fieldNames = {"data", "recebimentos", "pagamentos", "saldoDia", "saldoAcumulado"};
            String[] columnTitles = {"Data", "Recebimentos", "Pagamentos", "Saldo do Dia", "Saldo Acumulado"};

            for (String fieldName : fieldNames) {
                JRDesignField field = new JRDesignField();
                field.setName(fieldName);
                if ("data".equals(fieldName)) {
                    field.setValueClass(String.class);
                } else {
                    field.setValueClass(BigDecimal.class);
                }
                design.addField(field);
            }

            JRDesignBand titleBand = new JRDesignBand();
            titleBand.setHeight(60);
            JRDesignStaticText titleText = new JRDesignStaticText();
            titleText.setX(0);
            titleText.setY(0);
            titleText.setWidth(515);
            titleText.setHeight(24);
            titleText.setHorizontalTextAlign(HorizontalTextAlignEnum.LEFT);
            titleText.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
            titleText.setStyle(titleStyle);
            Empresa empresa = empresaService.getDadosEmpresa();
            String empresaLabel = "";
            if (empresa != null) {
                String nome = empresa.getNome() != null ? empresa.getNome() : "";
                String nif = empresa.getNif() != null ? empresa.getNif() : "";
                empresaLabel = nome;
                if (!nif.isBlank()) {
                    empresaLabel = empresaLabel + "  |  NIF " + nif;
                }
            }
            titleText.setText(empresaLabel.isBlank() ? "Fluxo de Caixa" : empresaLabel);
            titleBand.addElement(titleText);

            JRDesignStaticText periodText = new JRDesignStaticText();
            periodText.setX(0);
            periodText.setY(26);
            periodText.setWidth(515);
            periodText.setHeight(18);
            periodText.setHorizontalTextAlign(HorizontalTextAlignEnum.LEFT);
            periodText.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
            periodText.setStyle(normal);
            String periodo = inicio + " a " + fim;
            periodText.setText("Período: " + periodo);
            titleBand.addElement(periodText);

            design.setTitle(titleBand);

            JRDesignBand headerBand = new JRDesignBand();
            headerBand.setHeight(20);
            int totalWidth = 515;
            int columns = fieldNames.length;
            int[] widths = calcColumnWidths(columns, totalWidth);
            int x = 0;
            for (int i = 0; i < columnTitles.length; i++) {
                JRDesignStaticText h = new JRDesignStaticText();
                h.setX(x);
                h.setY(0);
                h.setWidth(widths[i]);
                h.setHeight(20);
                h.setHorizontalTextAlign(i == 0 ? HorizontalTextAlignEnum.LEFT : HorizontalTextAlignEnum.RIGHT);
                h.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
                h.setStyle(headerStyle);
                h.setText(columnTitles[i]);
                headerBand.addElement(h);
                x += widths[i];
            }
            design.setColumnHeader(headerBand);

            JRDesignBand detailBand = new JRDesignBand();
            detailBand.setHeight(18);
            x = 0;
            for (int i = 0; i < fieldNames.length; i++) {
                JRDesignExpression exp = new JRDesignExpression();
                exp.setText("$F{" + fieldNames[i] + "}");

                net.sf.jasperreports.engine.design.JRDesignTextField tf = new net.sf.jasperreports.engine.design.JRDesignTextField();
                tf.setX(x);
                tf.setY(0);
                tf.setWidth(widths[i]);
                tf.setHeight(18);
                tf.setHorizontalTextAlign(i == 0 ? HorizontalTextAlignEnum.LEFT : HorizontalTextAlignEnum.RIGHT);
                tf.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
                tf.setStyle(normal);
                if (i > 0) {
                    tf.setPattern("Kz #,##0.00");
                }
                tf.setExpression(exp);
                detailBand.addElement(tf);
                x += widths[i];
            }
            JRDesignSection detailSection = (JRDesignSection) design.getDetailSection();
            detailSection.addBand(detailBand);

            BigDecimal totalRecebimentos = BigDecimal.ZERO;
            BigDecimal totalPagamentos = BigDecimal.ZERO;
            BigDecimal saldoFinal = BigDecimal.ZERO;
            for (RowFluxoCaixa r : rows) {
                totalRecebimentos = totalRecebimentos.add(r.getRecebimentos());
                totalPagamentos = totalPagamentos.add(r.getPagamentos());
                saldoFinal = r.getSaldoAcumulado();
            }
            BigDecimal saldoLiquido = totalRecebimentos.subtract(totalPagamentos);

            JRDesignBand footerBand = new JRDesignBand();
            footerBand.setHeight(40);
            JRDesignStaticText footerText = new JRDesignStaticText();
            footerText.setX(0);
            footerText.setY(0);
            footerText.setWidth(515);
            footerText.setHeight(20);
            footerText.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
            footerText.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
            footerText.setStyle(normal);
            String resumo = "Totais  |  Recebimentos " + fmt(totalRecebimentos)
                    + "   Pagamentos " + fmt(totalPagamentos)
                    + "   Saldo Líquido " + fmt(saldoLiquido)
                    + "   Saldo Final " + fmt(saldoFinal);
            footerText.setText(resumo);
            footerBand.addElement(footerText);

            JRDesignStaticText metaText = new JRDesignStaticText();
            metaText.setX(0);
            metaText.setY(20);
            metaText.setWidth(515);
            metaText.setHeight(18);
            metaText.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
            metaText.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
            metaText.setStyle(normal);
            String meta = "Gerado em " + java.time.LocalDate.now() + " às " + java.time.LocalTime.now().withNano(0);
            metaText.setText(meta);
            footerBand.addElement(metaText);
            design.setColumnFooter(footerBand);

            net.sf.jasperreports.engine.JasperReport report = JasperCompileManager.compileReport(design);
            JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(rows);
            JasperPrint jp = JasperFillManager.fillReport(report, params, ds);
            jp.setName("Fluxo_Caixa_" + inicio + "_a_" + fim);
            return jp;
        } catch (JRException e) {
            throw new RuntimeException("Erro ao gerar relatório de Fluxo de Caixa: " + e.getMessage(), e);
        }
    }

    private List<RowFluxoCaixa> buildRows(LocalDate inicio, LocalDate fim) {
        List<RowFluxoCaixa> rows = new ArrayList<>();
        BigDecimal saldoAcumulado = BigDecimal.ZERO;

        LocalDate data = inicio;
        while (!data.isAfter(fim)) {
            BigDecimal recebimentos = reciboService.findByPeriodo(data, data).stream()
                    .map(Recibo::getValor)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal pagamentos = despesaService.sumPagoPeriodo(data, data);
            if (pagamentos == null) {
                pagamentos = BigDecimal.ZERO;
            }

            BigDecimal saldoDia = recebimentos.subtract(pagamentos);
            saldoAcumulado = saldoAcumulado.add(saldoDia);

            rows.add(new RowFluxoCaixa(
                    data.toString(),
                    recebimentos,
                    pagamentos,
                    saldoDia,
                    saldoAcumulado
            ));

            data = data.plusDays(1);
        }

        return rows;
    }

    private Map<String, Object> getEmpresaParams() {
        Empresa empresa = empresaService.getDadosEmpresa();
        Map<String, Object> params = new HashMap<>();
        params.put("EMPRESA_NOME", empresa.getNome());
        params.put("EMPRESA_NIF", empresa.getNif());
        params.put("EMPRESA_ENDERECO", empresa.getEndereco());
        params.put("EMPRESA_TELEFONE", empresa.getTelefone());
        params.put("EMPRESA_EMAIL", empresa.getEmail());
        if (empresa.getLogotipo() != null && empresa.getLogotipo().length > 0) {
            params.put("EMPRESA_LOGO", new java.io.ByteArrayInputStream(empresa.getLogotipo()));
        }
        return params;
    }

    private int[] calcColumnWidths(int columns, int totalWidth) {
        int[] w = new int[columns];
        int avg = totalWidth / columns;
        for (int i = 0; i < columns; i++) {
            w[i] = avg;
        }
        return w;
    }

    private static String fmt(BigDecimal v) {
        return String.format("Kz %.2f", v != null ? v : BigDecimal.ZERO);
    }

    public static class RowFluxoCaixa {
        private final String data;
        private final BigDecimal recebimentos;
        private final BigDecimal pagamentos;
        private final BigDecimal saldoDia;
        private final BigDecimal saldoAcumulado;

        public RowFluxoCaixa(String data, BigDecimal recebimentos, BigDecimal pagamentos, BigDecimal saldoDia, BigDecimal saldoAcumulado) {
            this.data = data;
            this.recebimentos = recebimentos;
            this.pagamentos = pagamentos;
            this.saldoDia = saldoDia;
            this.saldoAcumulado = saldoAcumulado;
        }

        public String getData() {
            return data;
        }

        public BigDecimal getRecebimentos() {
            return recebimentos;
        }

        public BigDecimal getPagamentos() {
            return pagamentos;
        }

        public BigDecimal getSaldoDia() {
            return saldoDia;
        }

        public BigDecimal getSaldoAcumulado() {
            return saldoAcumulado;
        }
    }
}
