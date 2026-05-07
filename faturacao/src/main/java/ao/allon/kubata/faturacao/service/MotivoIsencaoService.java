package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.MotivoIsencao;
import ao.allon.kubata.faturacao.repository.MotivoIsencaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MotivoIsencaoService {

    private final MotivoIsencaoRepository repository;

    public MotivoIsencaoService(MotivoIsencaoRepository repository) {
        this.repository = repository;
    }

    public List<MotivoIsencao> findAll() {
        return repository.findAll();
    }

    public Optional<MotivoIsencao> findByCodigo(String codigo) {
        return repository.findByCodigo(codigo);
    }

    public List<MotivoIsencao> findActive() {
        return repository.findAll();
    }

    @Transactional
    public MotivoIsencao save(MotivoIsencao motivo) {
        return repository.save(motivo);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
