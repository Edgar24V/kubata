package ao.allon.kubata.faturacao.config;

import ao.allon.kubata.faturacao.domain.Imposto;
import ao.allon.kubata.faturacao.domain.MotivoIsencao;
import ao.allon.kubata.faturacao.repository.ImpostoRepository;
import ao.allon.kubata.faturacao.repository.MotivoIsencaoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ImpostoDataInitializer implements CommandLineRunner {

    private final ImpostoRepository impostoRepository;
    private final MotivoIsencaoRepository motivoIsencaoRepository;

    public ImpostoDataInitializer(ImpostoRepository impostoRepository, MotivoIsencaoRepository motivoIsencaoRepository) {
        this.impostoRepository = impostoRepository;
        this.motivoIsencaoRepository = motivoIsencaoRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (motivoIsencaoRepository.count() == 0) {
            createMotivo("M00", "Regime Simplificado", "Lei do IVA");
            createMotivo("M02", "Transmissão de bens e serviço não sujeita", "Artigo 2.º do CIVA");
            createMotivo("M04", "Isento Artigo 12.º do CIVA", "Artigo 12.º do CIVA");
            createMotivo("M10", "Isento Artigo 13.º do CIVA", "Artigo 13.º do CIVA");
            createMotivo("M11", "Isento Artigo 15.º do CIVA", "Artigo 15.º do CIVA");
        }

        if (impostoRepository.count() == 0) {
            createImposto("NOR", "Taxa Normal", new BigDecimal("14.00"), null);
            createImposto("RED", "Taxa Reduzida", new BigDecimal("7.00"), null);
            createImposto("AGR", "Insumos Agrícolas", new BigDecimal("5.00"), null);
            
            MotivoIsencao m00 = motivoIsencaoRepository.findByCodigo("M00").orElse(null);
            createImposto("ISE", "Isento", new BigDecimal("0.00"), m00);
            
            System.out.println("Impostos padrão de Angola inicializados.");
        }
    }

    private void createMotivo(String codigo, String descricao, String legislacao) {
        MotivoIsencao m = new MotivoIsencao();
        m.setCodigo(codigo);
        m.setDescricao(descricao);
        m.setLegislacao(legislacao);
        motivoIsencaoRepository.save(m);
    }

    private void createImposto(String codigo, String descricao, BigDecimal percentual, MotivoIsencao motivo) {
        Imposto imposto = new Imposto();
        imposto.setCodigo(codigo);
        imposto.setDescricao(descricao);
        imposto.setTipo("IVA");
        imposto.setPercentual(percentual);
        imposto.setMotivoIsencao(motivo);
        impostoRepository.save(imposto);
    }
}
