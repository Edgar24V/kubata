package ao.allon.kubata.rh.service;

import ao.allon.kubata.rh.domain.Colaborador;
import ao.allon.kubata.rh.domain.Departamento;
import ao.allon.kubata.rh.domain.Cargo;
import ao.allon.kubata.rh.repository.ColaboradorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ColaboradorService {

    @Autowired
    private ColaboradorRepository repository;

    @Autowired
    private DepartamentoService departamentoService;

    @Autowired
    private CargoService cargoService;

    @Transactional(readOnly = true)
    public List<Colaborador> findAll() {
        return repository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public List<Colaborador> findAtivos() {
        return repository.findByEstadoOrderByNomeCompleto(Colaborador.EstadoColaborador.ATIVO);
    }

    @Transactional(readOnly = true)
    public List<Colaborador> findByEstado(Colaborador.EstadoColaborador estado) {
        return repository.findByEstadoOrderByNomeCompleto(estado);
    }

    @Transactional(readOnly = true)
    public Optional<Colaborador> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Colaborador> findByBi(String bi) {
        return repository.findByBiIgnoreCase(bi);
    }

    @Transactional(readOnly = true)
    public Optional<Colaborador> findByNif(String nif) {
        return repository.findByNifIgnoreCase(nif);
    }

    @Transactional(readOnly = true)
    public Optional<Colaborador> findByNumeroMecanografico(String numeroMecanografico) {
        return repository.findByNumeroMecanograficoIgnoreCase(numeroMecanografico);
    }

    public Colaborador save(@Valid Colaborador colaborador) {
        // Validações de negócio
        if (colaborador.getNomeCompleto() == null || colaborador.getNomeCompleto().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome completo é obrigatório");
        }

        if (colaborador.getDataAdmissao() == null) {
            throw new IllegalArgumentException("Data de admissão é obrigatória");
        }

        if (colaborador.getDataAdmissao().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Data de admissão não pode ser futura");
        }

        // Verifica duplicidade de BI
        if (colaborador.getBi() != null && !colaborador.getBi().trim().isEmpty()) {
            if (colaborador.getId() == null) {
                if (repository.existsByBiIgnoreCase(colaborador.getBi())) {
                    throw new IllegalArgumentException("Já existe um colaborador com este BI");
                }
            } else {
                Optional<Colaborador> existente = repository.findByBiIgnoreCase(colaborador.getBi());
                if (existente.isPresent() && !existente.get().getId().equals(colaborador.getId())) {
                    throw new IllegalArgumentException("Já existe um colaborador com este BI");
                }
            }
        }

        // Verifica duplicidade de NIF
        if (colaborador.getNif() != null && !colaborador.getNif().trim().isEmpty()) {
            if (colaborador.getId() == null) {
                if (repository.existsByNifIgnoreCase(colaborador.getNif())) {
                    throw new IllegalArgumentException("Já existe um colaborador com este NIF");
                }
            } else {
                Optional<Colaborador> existente = repository.findByNifIgnoreCase(colaborador.getNif());
                if (existente.isPresent() && !existente.get().getId().equals(colaborador.getId())) {
                    throw new IllegalArgumentException("Já existe um colaborador com este NIF");
                }
            }
        }

        // Gera número mecanográfico automático se não informado
        if (colaborador.getNumeroMecanografico() == null || colaborador.getNumeroMecanografico().trim().isEmpty()) {
            colaborador.setNumeroMecanografico(gerarNumeroMecanografico());
        } else {
            // Verifica duplicidade do número mecanográfico se informado
            if (colaborador.getId() == null) {
                if (repository.existsByNumeroMecanograficoIgnoreCase(colaborador.getNumeroMecanografico())) {
                    throw new IllegalArgumentException("Já existe um colaborador com este número mecanográfico");
                }
            } else {
                Optional<Colaborador> existente = repository.findByNumeroMecanograficoIgnoreCase(colaborador.getNumeroMecanografico());
                if (existente.isPresent() && !existente.get().getId().equals(colaborador.getId())) {
                    throw new IllegalArgumentException("Já existe um colaborador com este número mecanográfico");
                }
            }
        }

        // Valida departamento e cargo
        if (colaborador.getDepartamento() != null && colaborador.getDepartamento().getId() != null) {
            colaborador.setDepartamento(departamentoService.findById(colaborador.getDepartamento().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Departamento não encontrado")));
        }

        if (colaborador.getCargo() != null && colaborador.getCargo().getId() != null) {
            colaborador.setCargo(cargoService.findById(colaborador.getCargo().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Cargo não encontrado")));
        }

        // Define estado padrão se não informado
        if (colaborador.getEstado() == null) {
            colaborador.setEstado(Colaborador.EstadoColaborador.ATIVO);
        }

        return repository.save(colaborador);
    }

    public Colaborador update(Long id, @Valid Colaborador colaborador) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Colaborador não encontrado");
        }
        colaborador.setId(id);
        return save(colaborador);
    }

    public void desligar(Long id, LocalDate dataDesligamento, String motivo) {
        Colaborador colaborador = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Colaborador não encontrado"));

        if (dataDesligamento == null) {
            dataDesligamento = LocalDate.now();
        }

        if (dataDesligamento.isBefore(colaborador.getDataAdmissao())) {
            throw new IllegalArgumentException("Data de desligamento não pode ser anterior à data de admissão");
        }

        colaborador.setEstado(Colaborador.EstadoColaborador.DESLIGADO);
        colaborador.setDataDesligamento(dataDesligamento);
        colaborador.setMotivoDesligamento(motivo);

        repository.save(colaborador);
    }

    public void reativar(Long id) {
        Colaborador colaborador = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Colaborador não encontrado"));

        colaborador.setEstado(Colaborador.EstadoColaborador.ATIVO);
        colaborador.setDataDesligamento(null);
        colaborador.setMotivoDesligamento(null);

        repository.save(colaborador);
    }

    public void activate(Long id) {
        reativar(id);
    }

    public void deactivate(Long id) {
        desligar(id, LocalDate.now(), "Inativado via sistema");
    }

    @Transactional(readOnly = true)
    public List<Colaborador> findByFiltros(String nome, String bi, String nif, 
                                           Long departamentoId, Long cargoId, 
                                           Colaborador.EstadoColaborador estado) {
        return repository.findByFiltros(nome, bi, nif, departamentoId, cargoId, estado);
    }

    @Transactional(readOnly = true)
    public List<Colaborador> findContratosAExpirar(LocalDate dataInicio, LocalDate dataFim) {
        return repository.findContratosAExpirar(dataInicio, dataFim);
    }

    @Transactional(readOnly = true)
    public long countByEstadoAtivo() {
        return repository.countByEstadoAtivo();
    }

    @Transactional(readOnly = true)
    public long countByDepartamento(Long departamentoId) {
        return repository.countByDepartamentoAndEstadoAtivo(departamentoId);
    }

    @Transactional(readOnly = true)
    public long countByCargo(Long cargoId) {
        return repository.countByCargoAndEstadoAtivo(cargoId);
    }

    /**
     * Gera número mecanográfico automático
     */
    private String gerarNumeroMecanografico() {
        // Formato: COL + ano + sequencial de 4 dígitos
        String ano = String.valueOf(LocalDate.now().getYear());
        long count = repository.count() + 1;
        return "COL" + ano + String.format("%04d", count);
    }
}
