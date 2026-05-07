package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Imposto;
import ao.allon.kubata.faturacao.repository.ImpostoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ImpostoService {

    private final ImpostoRepository repository;

    public ImpostoService(ImpostoRepository repository) {
        this.repository = repository;
    }

    public List<Imposto> findAll() {
        return repository.findAll();
    }

    public List<Imposto> findActive() {
        return repository.findByActiveTrue();
    }

    public Optional<Imposto> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<Imposto> findByCodigo(String codigo) {
        return repository.findByCodigo(codigo);
    }

    @Transactional
    public Imposto save(Imposto imposto) {
        return repository.save(imposto);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
    
    @Transactional
    public void softDelete(Long id) {
        repository.findById(id).ifPresent(i -> {
            i.setActive(false);
            repository.save(i);
        });
    }
}
