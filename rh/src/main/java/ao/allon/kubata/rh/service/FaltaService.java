package ao.allon.kubata.rh.service;

import ao.allon.kubata.rh.domain.Falta;
import ao.allon.kubata.rh.domain.Colaborador;
import ao.allon.kubata.rh.repository.FaltaRepository;
import ao.allon.kubata.rh.repository.ColaboradorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FaltaService {

    @Autowired
    private FaltaRepository repository;

    @Autowired
    private ColaboradorRepository colaboradorRepository;

    public List<Falta> findAll() {
        return repository.findAll();
    }

    public Optional<Falta> findById(Long id) {
        return repository.findById(id);
    }

    public Falta save(Falta falta) {
        if (falta.getColaborador() == null || falta.getColaborador().getId() == null) {
            throw new IllegalArgumentException("Colaborador é obrigatório");
        }

        if (falta.getData() == null) {
            throw new IllegalArgumentException("Data é obrigatória");
        }

        if (falta.getTipo() == null) {
            throw new IllegalArgumentException("Tipo de falta é obrigatório");
        }

        // Verifica se colaborador existe
        Colaborador colaborador = colaboradorRepository.findById(falta.getColaborador().getId())
            .orElseThrow(() -> new IllegalArgumentException("Colaborador não encontrado"));

        falta.setColaborador(colaborador);

        return repository.save(falta);
    }

    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Falta não encontrada");
        }
        repository.deleteById(id);
    }

    public List<Falta> findByData(LocalDate data) {
        return repository.findByDataBetweenOrderByData(data, data);
    }

    public List<Falta> findByDataBetween(LocalDate dataInicio, LocalDate dataFim) {
        return repository.findByDataBetweenOrderByData(dataInicio, dataFim);
    }

    public List<Falta> findByColaborador(Long colaboradorId) {
        return repository.findByColaboradorIdOrderByDataDesc(colaboradorId);
    }

    public List<Falta> findByColaboradorAndDataBetween(Long colaboradorId, LocalDate dataInicio, LocalDate dataFim) {
        return repository.findByColaboradorIdAndDataBetweenOrderByData(colaboradorId, dataInicio, dataFim);
    }

    public List<Falta> findByTipo(Falta.TipoFalta tipo) {
        return repository.findByTipoOrderByDataDesc(tipo);
    }

    public List<Falta> findByJustificada(Boolean justificada) {
        if (justificada) {
            return repository.findByJustificadaTrueOrderByDataDesc();
        } else {
            return repository.findByJustificadaFalseOrderByDataDesc();
        }
    }

    public List<Falta> findByFiltros(Long colaboradorId, Falta.TipoFalta tipo, 
                                    LocalDate dataInicio, LocalDate dataFim, Boolean justificada) {
        return repository.findByFiltros(colaboradorId, dataInicio, dataFim, tipo, justificada);
    }

    public void justificar(Long id, LocalDate dataJustificacao, String observacoes) {
        Falta falta = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Falta não encontrada"));

        falta.setJustificada(true);
        falta.setDataJustificacao(dataJustificacao != null ? dataJustificacao : LocalDate.now());
        if (observacoes != null) {
            falta.setObservacoes(observacoes);
        }

        repository.save(falta);
    }

    public void desjustificar(Long id) {
        Falta falta = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Falta não encontrada"));

        falta.setJustificada(false);
        falta.setDataJustificacao(null);

        repository.save(falta);
    }

    public long countByData(LocalDate data) {
        return repository.countByDataBetween(data, data);
    }

    public long countByDataBetween(LocalDate dataInicio, LocalDate dataFim) {
        return repository.countByDataBetween(dataInicio, dataFim);
    }

    public long countByColaboradorAndDataBetween(Long colaboradorId, LocalDate dataInicio, LocalDate dataFim) {
        return repository.countByColaboradorAndDataBetween(colaboradorId, dataInicio, dataFim);
    }

    public List<Falta> findFaltasNaoJustificadas(Long colaboradorId, LocalDate dataInicio, LocalDate dataFim) {
        // TODO: Implementar consulta específica
        return repository.findByColaboradorIdAndDataBetweenOrderByData(colaboradorId, dataInicio, dataFim)
            .stream()
            .filter(f -> !f.getJustificada())
            .toList();
    }
}
