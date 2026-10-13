package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.ExercicioFiscalRepository;
import ao.allon.kubata.core.repository.ParametroSistemaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmpresaSetupServiceValidationTest {

    private final EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
    private final ExercicioFiscalRepository exercicioRepository = mock(ExercicioFiscalRepository.class);
    private final ParametroSistemaRepository parametroRepository = mock(ParametroSistemaRepository.class);
    private final EmpresaSetupService service =
            new EmpresaSetupService(empresaRepository, exercicioRepository, parametroRepository);

    @BeforeEach
    void setUp() {
        when(empresaRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void rejeitaIdentificadorComMaisOuMenosDeTresLetras() {
        Empresa empresa = empresaValida();
        empresa.setIdentificador("KB");

        assertThrows(IllegalArgumentException.class, () -> provisionar(empresa));
    }

    @Test
    void rejeitaMoedaAlternativaIgualAMoedaBase() {
        Empresa empresa = empresaValida();
        empresa.setMoedaAlternativa("AOA");
        empresa.setTaxaCambio(new BigDecimal("920.000000"));

        assertThrows(IllegalArgumentException.class, () -> provisionar(empresa));
    }

    @Test
    void exigeTaxaPositivaQuandoExisteMoedaAlternativa() {
        Empresa empresa = empresaValida();
        empresa.setMoedaAlternativa("USD");
        empresa.setTaxaCambio(BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> provisionar(empresa));
    }

    @Test
    void rejeitaPeriodicidadeIvaDesconhecida() {
        Empresa empresa = empresaValida();
        empresa.setPeriodicidadeIva("SEMANAL");

        assertThrows(IllegalArgumentException.class, () -> provisionar(empresa));
    }

    @Test
    void rejeitaSiglaJaAtribuidaAOutraEmpresaMesmoComMaiusculasDiferentes() {
        Empresa existente = empresaValida();
        existente.setId(10L);
        existente.setIdentificador("kbt");
        when(empresaRepository.findAll()).thenReturn(List.of(existente));

        Empresa nova = empresaValida();
        nova.setId(null);
        nova.setIdentificador("KBT");

        assertThrows(IllegalArgumentException.class, () -> provisionar(nova));
    }

    private void provisionar(Empresa empresa) {
        service.saveAndProvision(
                empresa,
                false,
                true,
                "DAILY",
                30,
                false,
                Map.of(),
                "Teste"
        );
    }

    private Empresa empresaValida() {
        Empresa empresa = Empresa.builder()
                .nome("Empresa de Teste")
                .nif("123456789")
                .identificador("KBT")
                .tipoContribuinte("Pessoa Colectiva")
                .pais("AO")
                .provincia("Luanda")
                .municipio("Luanda")
                .regimeFiscal("Regime Geral")
                .moedaBase("AOA")
                .tipoInstalacao("STANDARD")
                .sentidoCambio("INVERTIDO")
                .periodicidadeIva("MENSAL")
                .exercicioActual(2026)
                .ativa(true)
                .predefinida(false)
                .build();
        return empresa;
    }
}
