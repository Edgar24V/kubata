package ao.allon.kubata.inventario.service;

import ao.allon.kubata.inventario.domain.Armazem;
import ao.allon.kubata.inventario.repository.ArmazemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ArmazemService {
    private final ArmazemRepository repository;

    public ArmazemService(ArmazemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Armazem> findAll() {
        return repository.findAll();
    }

    public Armazem save(Armazem armazem) {
        if (armazem.getNome() == null || armazem.getNome().isBlank()) {
            throw new IllegalArgumentException("O nome do armazém é obrigatório.");
        }
        if (armazem.getIsPrincipal() == null) {
            armazem.setIsPrincipal(false);
        }
        if (Boolean.TRUE.equals(armazem.getIsPrincipal())) {
            repository.findByIsPrincipalTrue().ifPresent(current -> {
                if (!current.getId().equals(armazem.getId())) {
                    current.setIsPrincipal(false);
                    repository.save(current);
                }
            });
        }
        return repository.save(armazem);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
