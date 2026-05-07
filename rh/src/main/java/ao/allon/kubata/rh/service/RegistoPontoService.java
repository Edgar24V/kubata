package ao.allon.kubata.rh.service;

import ao.allon.kubata.rh.domain.RegistoPonto;
import ao.allon.kubata.rh.domain.Colaborador;
import ao.allon.kubata.rh.repository.RegistoPontoRepository;
import ao.allon.kubata.rh.repository.ColaboradorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RegistoPontoService {

    @Autowired
    private RegistoPontoRepository repository;

    @Autowired
    private ColaboradorRepository colaboradorRepository;

    public List<RegistoPonto> findAll() {
        return repository.findAll();
    }

    public Optional<RegistoPonto> findById(Long id) {
        return repository.findById(id);
    }

    public RegistoPonto save(RegistoPonto registo) {
        if (registo.getColaborador() == null || registo.getColaborador().getId() == null) {
            throw new IllegalArgumentException("Colaborador é obrigatório");
        }

        if (registo.getData() == null) {
            throw new IllegalArgumentException("Data é obrigatória");
        }

        // Verifica se colaborador existe
        Colaborador colaborador = colaboradorRepository.findById(registo.getColaborador().getId())
            .orElseThrow(() -> new IllegalArgumentException("Colaborador não encontrado"));

        registo.setColaborador(colaborador);

        return repository.save(registo);
    }

    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Registro não encontrado");
        }
        repository.deleteById(id);
    }

    public List<RegistoPonto> findByData(LocalDate data) {
        return repository.findByDataBetweenOrderByData(data, data);
    }

    public List<RegistoPonto> findByDataBetween(LocalDate dataInicio, LocalDate dataFim) {
        return repository.findByDataBetweenOrderByData(dataInicio, dataFim);
    }

    public Optional<RegistoPonto> findByColaboradorAndData(Long colaboradorId, LocalDate data) {
        return repository.findByColaboradorIdAndData(colaboradorId, data);
    }

    public List<RegistoPonto> findByColaborador(Long colaboradorId) {
        return repository.findByColaboradorIdOrderByDataDesc(colaboradorId);
    }

    public List<RegistoPonto> findByColaboradorAndDataBetween(Long colaboradorId, LocalDate dataInicio, LocalDate dataFim) {
        return repository.findByColaboradorIdAndDataBetweenOrderByData(colaboradorId, dataInicio, dataFim);
    }

    public List<RegistoPonto> findByAprovado(Boolean aprovado) {
        // TODO: Implementar consulta por status de aprovação quando houver método no repository
        return repository.findAll();
    }

    public void aprovar(Long id, Long aprovadoPor) {
        RegistoPonto registo = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Registro não encontrado"));

        // TODO: Buscar usuário real quando sistema de autenticação estiver pronto
        // registo.setAprovadoPor(userRepository.findById(aprovadoPor).orElse(null));
        repository.save(registo);
    }

    public void desaprovar(Long id) {
        RegistoPonto registo = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Registro não encontrado"));

        registo.setAprovadoPor(null);
        repository.save(registo);
    }

    public long countByData(LocalDate data) {
        return repository.countByDataBetween(data, data);
    }

    public long countByDataBetween(LocalDate dataInicio, LocalDate dataFim) {
        return repository.countByDataBetween(dataInicio, dataFim);
    }
}
