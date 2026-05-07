package ao.allon.kubata.rh.service;

import ao.allon.kubata.rh.domain.Cargo;
import ao.allon.kubata.rh.repository.CargoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CargoService {

    @Autowired
    private CargoRepository repository;

    @Transactional(readOnly = true)
    public List<Cargo> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Cargo> findAtivos() {
        return repository.findByActiveTrueOrderByNome();
    }

    @Transactional(readOnly = true)
    public List<Cargo> findInativos() {
        return repository.findByActiveFalseOrderByNome();
    }

    @Transactional(readOnly = true)
    public Optional<Cargo> findById(Long id) {
        return repository.findById(id);
    }

    public Cargo save(@Valid Cargo cargo) {
        // Validações de negócio
        if (cargo.getNome() == null || cargo.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do cargo é obrigatório");
        }

        // Verifica duplicidade de nome
        if (cargo.getId() == null) {
            if (repository.existsByNomeIgnoreCase(cargo.getNome())) {
                throw new IllegalArgumentException("Já existe um cargo com este nome");
            }
        } else {
            if (repository.existsByNomeIgnoreCase(cargo.getNome())) {
                throw new IllegalArgumentException("Já existe um cargo com este nome");
            }
        }

        return repository.save(cargo);
    }

    public Cargo update(Long id, @Valid Cargo cargo) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Cargo não encontrado");
        }
        cargo.setId(id);
        return save(cargo);
    }

    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Cargo não encontrado");
        }

        // TODO: Verificar se existem colaboradores vinculados antes de desativar
        Cargo cargo = repository.findById(id).orElseThrow();
        cargo.setActive(false);
        repository.save(cargo);
    }

    public void activate(Long id) {
        Cargo cargo = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cargo não encontrado"));
        cargo.setActive(true);
        repository.save(cargo);
    }

    public void deactivate(Long id) {
        Cargo cargo = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cargo não encontrado"));
        cargo.setActive(false);
        repository.save(cargo);
    }

    @Transactional(readOnly = true)
    public List<Cargo> findByFiltros(String nome, String nivel, Boolean ativo) {
        return repository.findByFiltros(nome, nivel, ativo);
    }

    @Transactional(readOnly = true)
    public long countAtivos() {
        return repository.countByActiveTrue();
    }

    @Transactional(readOnly = true)
    public long countInativos() {
        return repository.count() - repository.countByActiveTrue();
    }
}
