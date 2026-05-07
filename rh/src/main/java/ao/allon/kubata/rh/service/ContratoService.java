package ao.allon.kubata.rh.service;

import ao.allon.kubata.rh.domain.Contrato;
import ao.allon.kubata.rh.domain.Colaborador;
import ao.allon.kubata.rh.repository.ContratoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ContratoService {

    @Autowired
    private ContratoRepository repository;

    @Autowired
    private ColaboradorService colaboradorService;

    @Transactional(readOnly = true)
    public List<Contrato> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Contrato> findByColaborador(Long colaboradorId) {
        return repository.findByColaboradorIdOrderByDataInicioDesc(colaboradorId);
    }

    @Transactional(readOnly = true)
    public List<Contrato> findBySituacao(Contrato.SituacaoContrato situacao) {
        return repository.findBySituacaoOrderByDataInicioDesc(situacao);
    }

    @Transactional(readOnly = true)
    public Optional<Contrato> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Contrato> findContratoVigente(Long colaboradorId, LocalDate data) {
        return repository.findContratoVigente(colaboradorId, data != null ? data : LocalDate.now());
    }

    @Transactional(readOnly = true)
    public Optional<Contrato> findUltimoContrato(Long colaboradorId) {
        return repository.findFirstByColaboradorIdOrderByDataInicioDesc(colaboradorId);
    }

    public Contrato save(@Valid Contrato contrato) {
        // Validações de negócio
        if (contrato.getColaborador() == null || contrato.getColaborador().getId() == null) {
            throw new IllegalArgumentException("Colaborador é obrigatório");
        }

        if (contrato.getDataInicio() == null) {
            throw new IllegalArgumentException("Data de início é obrigatória");
        }

        if (contrato.getDataInicio().isAfter(LocalDate.now().plusDays(30))) {
            throw new IllegalArgumentException("Data de início não pode ser superior a 30 dias no futuro");
        }

        if (contrato.getTipoContrato() == null) {
            throw new IllegalArgumentException("Tipo de contrato é obrigatório");
        }

        // Valida data fim para contratos termo certo
        if (contrato.getTipoContrato() == Contrato.TipoContrato.TERMO_CERTO && contrato.getDataFim() == null) {
            throw new IllegalArgumentException("Contrato termo certo deve ter data fim definida");
        }

        // Valida relação entre datas
        if (contrato.getDataFim() != null && contrato.getDataFim().isBefore(contrato.getDataInicio())) {
            throw new IllegalArgumentException("Data fim não pode ser anterior à data início");
        }

        // Valida salário base se informado
        if (contrato.getSalarioBase() != null && contrato.getSalarioBase().doubleValue() < 0) {
            throw new IllegalArgumentException("Salário base não pode ser negativo");
        }

        // Busca colaborador válido
        Colaborador colaborador = colaboradorService.findById(contrato.getColaborador().getId())
                .orElseThrow(() -> new IllegalArgumentException("Colaborador não encontrado"));

        // Verifica se já existe contrato ativo no mesmo período
        List<Contrato> contratosExistentes = repository.findByColaboradorIdOrderByDataInicioDesc(colaborador.getId());
        for (Contrato existente : contratosExistentes) {
            if (existente.getId() != null && !existente.getId().equals(contrato.getId()) &&
                existente.getSituacao() == Contrato.SituacaoContrato.ATIVO) {
                
                if (existeSobreposicaoDatas(contrato.getDataInicio(), contrato.getDataFim(), 
                                           existente.getDataInicio(), existente.getDataFim())) {
                    throw new IllegalArgumentException("Já existe um contrato ativo neste período");
                }
            }
        }

        contrato.setColaborador(colaborador);

        // Define situação padrão se não informado
        if (contrato.getSituacao() == null) {
            contrato.setSituacao(Contrato.SituacaoContrato.ATIVO);
        }

        return repository.save(contrato);
    }

    public Contrato update(Long id, @Valid Contrato contrato) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Contrato não encontrado");
        }
        contrato.setId(id);
        return save(contrato);
    }

    public void rescindir(Long id, LocalDate dataRescisao, String motivo) {
        Contrato contrato = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado"));

        if (dataRescisao == null) {
            dataRescisao = LocalDate.now();
        }

        if (dataRescisao.isBefore(contrato.getDataInicio())) {
            throw new IllegalArgumentException("Data de rescisão não pode ser anterior à data de início");
        }

        contrato.setSituacao(Contrato.SituacaoContrato.RESCINDIDO);
        contrato.setDataRescisao(dataRescisao);
        contrato.setMotivoRescisao(motivo);

        repository.save(contrato);
    }

    public void suspender(Long id) {
        Contrato contrato = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado"));

        contrato.setSituacao(Contrato.SituacaoContrato.SUSPENSO);
        repository.save(contrato);
    }

    public void reativar(Long id) {
        Contrato contrato = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado"));

        contrato.setSituacao(Contrato.SituacaoContrato.ATIVO);
        repository.save(contrato);
    }

    @Transactional(readOnly = true)
    public List<Contrato> findByFiltros(Long colaboradorId, Contrato.TipoContrato tipoContrato,
                                        Contrato.SituacaoContrato situacao, LocalDate dataInicio,
                                        LocalDate dataFim) {
        return repository.findByFiltros(colaboradorId, tipoContrato, situacao, dataInicio, dataFim);
    }

    @Transactional(readOnly = true)
    public List<Contrato> findContratosExpirando(LocalDate dataLimite) {
        return repository.findContratosAExpirar(LocalDate.now(), dataLimite);
    }

    @Transactional(readOnly = true)
    public List<Contrato> findContratosExpirados() {
        return repository.findContratosExpirados(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public long countBySituacaoAtivo() {
        return repository.countBySituacaoAtivo();
    }

    @Transactional(readOnly = true)
    public long countByTipoContrato(Contrato.TipoContrato tipoContrato) {
        return repository.countByTipoContratoAndSituacaoAtivo(tipoContrato);
    }

    /**
     * Verifica se existe sobreposição de períodos entre datas
     */
    private boolean existeSobreposicaoDatas(LocalDate inicio1, LocalDate fim1, 
                                           LocalDate inicio2, LocalDate fim2) {
        if (fim1 == null && fim2 == null) {
            return true; // Ambos indeterminados
        }
        if (fim1 == null) {
            return !inicio1.isAfter(fim2);
        }
        if (fim2 == null) {
            return !inicio2.isAfter(fim1);
        }
        return !inicio1.isAfter(fim2) && !inicio2.isAfter(fim1);
    }
}
