package ao.allon.kubata.rh.service;

import ao.allon.kubata.rh.domain.Departamento;
import ao.allon.kubata.rh.repository.DepartamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DepartamentoService {

    @Autowired
    private DepartamentoRepository repository;

    @Transactional(readOnly = true)
    public List<Departamento> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Departamento> findAtivos() {
        return repository.findByActiveTrueOrderByNome();
    }

    @Transactional(readOnly = true)
    public List<Departamento> findInativos() {
        return repository.findByActiveFalseOrderByNome();
    }

    @Transactional(readOnly = true)
    public Optional<Departamento> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Departamento> findBySigla(String sigla) {
        return repository.findBySiglaIgnoreCase(sigla);
    }

    public Departamento save(@Valid Departamento departamento) {
        // Validações de negócio
        if (departamento.getNome() == null || departamento.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do departamento é obrigatório");
        }

        // Verifica duplicidade de nome
        if (departamento.getId() == null) {
            if (repository.existsByNomeIgnoreCase(departamento.getNome())) {
                throw new IllegalArgumentException("Já existe um departamento com este nome");
            }
        } else {
            if (repository.existsByNomeIgnoreCase(departamento.getNome())) {
                throw new IllegalArgumentException("Já existe um departamento com este nome");
            }
        }

        // Verifica duplicidade de sigla se informada
        if (departamento.getSigla() != null && !departamento.getSigla().trim().isEmpty()) {
            if (departamento.getId() == null) {
                if (repository.existsBySiglaIgnoreCase(departamento.getSigla())) {
                    throw new IllegalArgumentException("Já existe um departamento com esta sigla");
                }
            } else {
                Optional<Departamento> existente = repository.findBySiglaIgnoreCase(departamento.getSigla());
                if (existente.isPresent() && !existente.get().getId().equals(departamento.getId())) {
                    throw new IllegalArgumentException("Já existe um departamento com esta sigla");
                }
            }
        }

        return repository.save(departamento);
    }

    public Departamento update(Long id, @Valid Departamento departamento) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Departamento não encontrado");
        }
        departamento.setId(id);
        return save(departamento);
    }

    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Departamento não encontrado");
        }

        // TODO: Verificar se existem colaboradores vinculados antes de desativar
        Departamento departamento = repository.findById(id).orElseThrow();
        departamento.setActive(false);
        repository.save(departamento);
    }

    public void activate(Long id) {
        Departamento departamento = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Departamento não encontrado"));
        departamento.setActive(true);
        repository.save(departamento);
    }

    public void deactivate(Long id) {
        Departamento departamento = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Departamento não encontrado"));
        departamento.setActive(false);
        repository.save(departamento);
    }

    @Transactional(readOnly = true)
    public List<Departamento> findByFiltros(String nome, String sigla, Boolean ativo) {
        return repository.findByFiltros(nome, sigla, ativo);
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
