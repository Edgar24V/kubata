package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Categoria;
import ao.allon.kubata.faturacao.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<Categoria> findAll() {
        return categoriaRepository.findAll();
    }

    public Optional<Categoria> findById(Long id) {
        return categoriaRepository.findById(id);
    }

    @Transactional
    public Categoria save(Categoria categoria) {
        if (categoria.getId() == null && categoriaRepository.existsByNome(categoria.getNome())) {
            throw new IllegalArgumentException("Já existe uma categoria com este nome.");
        }
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) throw new IllegalArgumentException("ID cannot be null");
        categoriaRepository.deleteById(id);
    }
}
