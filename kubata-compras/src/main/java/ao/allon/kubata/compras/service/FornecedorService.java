package ao.allon.kubata.compras.service;

import ao.allon.kubata.compras.domain.Fornecedor;
import ao.allon.kubata.compras.repository.FornecedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;

    public FornecedorService(FornecedorRepository fornecedorRepository) {
        this.fornecedorRepository = fornecedorRepository;
    }

    public Fornecedor save(Fornecedor fornecedor) {
        return fornecedorRepository.save(fornecedor);
    }

    public Optional<Fornecedor> findById(Long id) {
        return fornecedorRepository.findById(id);
    }

    public Optional<Fornecedor> findByNif(String nif) {
        return fornecedorRepository.findByNif(nif);
    }

    public List<Fornecedor> findAll() {
        return fornecedorRepository.findAll();
    }

    public List<Fornecedor> findByNome(String nome) {
        return fornecedorRepository.findByNomeContainingIgnoreCase(nome);
    }

    public void delete(Long id) {
        fornecedorRepository.deleteById(id);
    }

    public boolean existsByNif(String nif) {
        return fornecedorRepository.existsByNif(nif);
    }
}
