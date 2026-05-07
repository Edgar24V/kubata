package ao.allon.kubata.rh.service;

import ao.allon.kubata.rh.domain.HoraExtra;
import ao.allon.kubata.rh.domain.Colaborador;
import ao.allon.kubata.rh.repository.HoraExtraRepository;
import ao.allon.kubata.rh.repository.ColaboradorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class HoraExtraService {

    @Autowired
    private HoraExtraRepository repository;

    @Autowired
    private ColaboradorRepository colaboradorRepository;

    public List<HoraExtra> findAll() {
        return repository.findAll();
    }

    public Optional<HoraExtra> findById(Long id) {
        return repository.findById(id);
    }

    public HoraExtra save(HoraExtra horaExtra) {
        if (horaExtra.getColaborador() == null || horaExtra.getColaborador().getId() == null) {
            throw new IllegalArgumentException("Colaborador é obrigatório");
        }

        if (horaExtra.getData() == null) {
            throw new IllegalArgumentException("Data é obrigatória");
        }

        if (horaExtra.getQuantidadeHoras() == null || horaExtra.getQuantidadeHoras().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantidade de horas deve ser maior que zero");
        }

        if (horaExtra.getMotivo() == null) {
            throw new IllegalArgumentException("Motivo é obrigatório");
        }

        // Verifica se colaborador existe
        Colaborador colaborador = colaboradorRepository.findById(horaExtra.getColaborador().getId())
            .orElseThrow(() -> new IllegalArgumentException("Colaborador não encontrado"));

        horaExtra.setColaborador(colaborador);

        return repository.save(horaExtra);
    }

    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Registro de horas extra não encontrado");
        }
        repository.deleteById(id);
    }

    public List<HoraExtra> findByColaborador(Long colaboradorId) {
        return repository.findByColaboradorIdOrderByDataDesc(colaboradorId);
    }

    public List<HoraExtra> findByDataBetween(LocalDate dataInicio, LocalDate dataFim) {
        return repository.findByDataBetweenOrderByData(dataInicio, dataFim);
    }

    public List<HoraExtra> findByColaboradorAndDataBetween(Long colaboradorId, LocalDate dataInicio, LocalDate dataFim) {
        return repository.findByColaboradorIdAndDataBetweenOrderByData(colaboradorId, dataInicio, dataFim);
    }

    public List<HoraExtra> findByEstado(HoraExtra.EstadoHoraExtra estado) {
        return repository.findByEstadoOrderByDataDesc(estado);
    }

    public List<HoraExtra> findPendentes() {
        return repository.findByEstadoOrderByDataDesc(HoraExtra.EstadoHoraExtra.PENDENTE);
    }

    public void aprovar(Long id, Long aprovadoPor) {
        HoraExtra horaExtra = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Horas extra não encontradas"));

        horaExtra.setEstado(HoraExtra.EstadoHoraExtra.APROVADO);
        horaExtra.setDataAprovacao(LocalDate.now());
        // TODO: Set aprovadoPor when user authentication is ready
        repository.save(horaExtra);
    }

    public void rejeitar(Long id, String motivoRejeicao) {
        HoraExtra horaExtra = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Horas extra não encontradas"));

        horaExtra.setEstado(HoraExtra.EstadoHoraExtra.REJEITADO);
        horaExtra.setObservacoes(motivoRejeicao);
        repository.save(horaExtra);
    }

    public BigDecimal calcularTotalHorasPorColaborador(Long colaboradorId, LocalDate dataInicio, LocalDate dataFim) {
        BigDecimal total = repository.sumHorasAprovadasByColaboradorAndPeriodo(colaboradorId, dataInicio, dataFim);
        return total != null ? total : BigDecimal.ZERO;
    }
}
