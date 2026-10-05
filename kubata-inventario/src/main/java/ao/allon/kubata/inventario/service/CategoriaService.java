package ao.allon.kubata.inventario.service;

import ao.allon.kubata.inventario.domain.Categoria;
import ao.allon.kubata.inventario.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoriaService {
    private final CategoriaRepository repository;

    public CategoriaService(CategoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Categoria> findAll() {
        return repository.findAll();
    }

    public Categoria save(Categoria categoria) {
        if (categoria.getNome() == null || categoria.getNome().isBlank()) {
            throw new IllegalArgumentException("O nome da categoria é obrigatório.");
        }
        return repository.save(categoria);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
