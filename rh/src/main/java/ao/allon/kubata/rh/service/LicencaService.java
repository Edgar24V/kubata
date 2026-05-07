package ao.allon.kubata.rh.service;

import ao.allon.kubata.rh.domain.Licenca;
import ao.allon.kubata.rh.domain.Colaborador;
import ao.allon.kubata.rh.repository.LicencaRepository;
import ao.allon.kubata.rh.repository.ColaboradorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LicencaService {

    @Autowired
    private LicencaRepository repository;

    @Autowired
    private ColaboradorRepository colaboradorRepository;

    public List<Licenca> findAll() {
        return repository.findAll();
    }

    public Optional<Licenca> findById(Long id) {
        return repository.findById(id);
    }

    public Licenca save(Licenca licenca) {
        if (licenca.getColaborador() == null || licenca.getColaborador().getId() == null) {
            throw new IllegalArgumentException("Colaborador é obrigatório");
        }

        if (licenca.getDataInicio() == null) {
            throw new IllegalArgumentException("Data de início é obrigatória");
        }

        if (licenca.getTipo() == null) {
            throw new IllegalArgumentException("Tipo de licença é obrigatório");
        }

        // Calcula quantidade de dias automaticamente se data fim estiver definida
        if (licenca.getDataFim() != null) {
            long dias = ChronoUnit.DAYS.between(licenca.getDataInicio(), licenca.getDataFim()) + 1;
            licenca.setQuantidadeDias((int) dias);
        }

        // Verifica se colaborador existe
        Colaborador colaborador = colaboradorRepository.findById(licenca.getColaborador().getId())
            .orElseThrow(() -> new IllegalArgumentException("Colaborador não encontrado"));

        licenca.setColaborador(colaborador);

        return repository.save(licenca);
    }

    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Licença não encontrada");
        }
        
        Licenca licenca = repository.findById(id).orElse(null);
        if (licenca != null && licenca.getEstado() == Licenca.EstadoLicenca.APROVADA) {
            throw new IllegalArgumentException("Não é possível excluir uma licença já aprovada");
        }
        
        repository.deleteById(id);
    }

    public List<Licenca> findByColaborador(Long colaboradorId) {
        return repository.findByColaboradorIdOrderByDataInicioDesc(colaboradorId);
    }

    public List<Licenca> findByDataInicioBetween(LocalDate dataInicio, LocalDate dataFim) {
        return repository.findByDataInicioBetweenOrderByDataInicio(dataInicio, dataFim);
    }

    public List<Licenca> findByTipo(Licenca.TipoLicenca tipo) {
        return repository.findByTipoOrderByDataInicioDesc(tipo);
    }

    public List<Licenca> findByEstado(Licenca.EstadoLicenca estado) {
        return repository.findByEstadoOrderByDataInicioDesc(estado);
    }

    public List<Licenca> findPendentes() {
        return repository.findByEstadoOrderByDataInicioDesc(Licenca.EstadoLicenca.PENDENTE);
    }

    public void aprovar(Long id, Long aprovadoPor) {
        Licenca licenca = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Licença não encontrada"));

        if (licenca.getEstado() != Licenca.EstadoLicenca.PENDENTE) {
            throw new IllegalArgumentException("Apenas licenças pendentes podem ser aprovadas");
        }

        licenca.setEstado(Licenca.EstadoLicenca.APROVADA);
        licenca.setDataAprovacao(LocalDate.now());
        // TODO: Set aprovadoPor when user authentication is ready
        repository.save(licenca);
    }

    public void rejeitar(Long id, String motivoRejeicao, Long rejeitadoPor) {
        Licenca licenca = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Licença não encontrada"));

        if (licenca.getEstado() != Licenca.EstadoLicenca.PENDENTE) {
            throw new IllegalArgumentException("Apenas licenças pendentes podem ser rejeitadas");
        }

        licenca.setEstado(Licenca.EstadoLicenca.REJEITADA);
        licenca.setMotivoRejeicao(motivoRejeicao);
        repository.save(licenca);
    }

    public void cancelar(Long id) {
        Licenca licenca = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Licença não encontrada"));

        if (licenca.getEstado() == Licenca.EstadoLicenca.APROVADA && 
            licenca.getDataInicio().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Não é possível cancelar licenças já iniciadas");
        }

        licenca.setEstado(Licenca.EstadoLicenca.CANCELADA);
        repository.save(licenca);
    }

    public List<Licenca> findByFiltros(Long colaboradorId, Licenca.TipoLicenca tipo, 
                                       Licenca.EstadoLicenca estado, LocalDate dataInicio, LocalDate dataFim) {
        return repository.findByFiltros(colaboradorId, tipo, estado, dataInicio, dataFim);
    }
}
