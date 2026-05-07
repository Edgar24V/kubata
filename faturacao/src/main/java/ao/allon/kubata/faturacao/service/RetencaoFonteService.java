package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.RetencaoFonte;
import ao.allon.kubata.faturacao.repository.RetencaoFonteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RetencaoFonteService {

    private final RetencaoFonteRepository repository;

    public RetencaoFonteService(RetencaoFonteRepository repository) {
        this.repository = repository;
    }

    public List<RetencaoFonte> findAll() {
        return repository.findAll();
    }
    
    public List<RetencaoFonte> findActive() {
        return repository.findActive();
    }

    public Optional<RetencaoFonte> findById(Long id) {
        return repository.findById(id);
    }
    
    public Optional<RetencaoFonte> findByCodigo(String codigo) {
        return repository.findByCodigo(codigo);
    }

    @Transactional
    public RetencaoFonte save(RetencaoFonte retencao) {
        return repository.save(retencao);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
    
    @Transactional
    public void softDelete(Long id) {
        repository.findById(id).ifPresent(r -> {
            r.setActive(false);
            repository.save(r);
        });
    }
}
