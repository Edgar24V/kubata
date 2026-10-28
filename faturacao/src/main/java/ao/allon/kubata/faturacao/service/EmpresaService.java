package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.repository.EmpresaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final ao.allon.kubata.core.service.LaunchContext launchContext;

    @Value("${kubata.empresa.nome:Minha Empresa}")
    private String defaultNome = "Minha Empresa";

    @Value("${kubata.empresa.nif:999999999}")
    private String defaultNif = "999999999";

    @Value("${kubata.empresa.endereco:Endereço da Empresa}")
    private String defaultEndereco = "Endereço da Empresa";

    @Value("${kubata.empresa.cidade:Luanda}")
    private String defaultCidade = "Luanda";

    public EmpresaService(EmpresaRepository empresaRepository,
                          ao.allon.kubata.core.service.LaunchContext launchContext) {
        this.empresaRepository = empresaRepository;
        this.launchContext = launchContext;
    }

    @Transactional(readOnly = true)
    public Empresa getDadosEmpresa() {
        return empresaDoContexto().orElseGet(this::empresaPadrao);
    }

    @Transactional(readOnly = true)
    public int getExercicioActual() {
        Optional<Integer> transmitido = launchContext.exercicioAno();
        if (transmitido.isPresent()) {
            return transmitido.get();
        }
        Integer ano = empresaDoContexto().map(Empresa::getExercicioActual).orElse(null);
        return ano != null ? ano : LocalDate.now().getYear();
    }

    @Transactional
    public Empresa salvarDadosEmpresa(Empresa empresa) {
        Empresa actual = empresaDoContexto().orElse(null);
        if (actual != null) {
            if (empresa.getId() == null || !empresa.getId().equals(actual.getId())) {
                empresa.setId(actual.getId());
            }
        } else if (empresa.getId() == null) {
            empresa.setPredefinida(true);
        }
        return empresaRepository.save(empresa);
    }

    /**
     * A empresa escolhida no Kubata Administrator tem prioridade; sem bilhete de sessão usa-se
     * a empresa predefinida e, na ausência desta, a primeira empresa activa.
     */
    @Transactional(readOnly = true)
    public Optional<Empresa> empresaDoContexto() {
        Optional<Long> doBilhete = launchContext.empresaId();
        if (doBilhete.isPresent()) {
            Optional<Empresa> transmitida = empresaRepository.findById(doBilhete.get());
            if (transmitida.isPresent()) {
                return transmitida;
            }
        }
        Optional<Empresa> predefinida = empresaRepository.findFirstByPredefinidaTrueAndAtivaTrue();
        return predefinida.isPresent() ? predefinida : empresaRepository.findFirstByAtivaTrue();
    }

    private Empresa empresaPadrao() {
        Empresa empresa = new Empresa();
        empresa.setNome(defaultNome);
        empresa.setNif(defaultNif);
        empresa.setMorada(defaultEndereco);
        empresa.setLocalidade(defaultCidade);
        return empresa;
    }
}
