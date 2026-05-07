package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Fornecedor;
import ao.allon.kubata.faturacao.repository.FornecedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;

    public FornecedorService(FornecedorRepository fornecedorRepository) {
        this.fornecedorRepository = fornecedorRepository;
    }

    public List<Fornecedor> findAll() {
        return fornecedorRepository.findAll();
    }

    public Optional<Fornecedor> findById(Long id) {
        return fornecedorRepository.findById(id);
    }

    @Transactional
    public Fornecedor save(Fornecedor fornecedor) {
        String nif = fornecedor.getNif();
        if (nif != null && !nif.isBlank()) {
            String msg = ao.allon.kubata.faturacao.util.AngolaValidationUtils.validateNifMessage(nif);
            if (msg != null) {
                throw new IllegalArgumentException(msg);
            }
        }
        if (fornecedor.getId() == null) {
            if (fornecedor.getNif() != null && fornecedorRepository.existsByNif(fornecedor.getNif())) {
                throw new IllegalArgumentException("Já existe fornecedor com NIF informado.");
            }
            if (fornecedorRepository.existsByNome(fornecedor.getNome())) {
                throw new IllegalArgumentException("Já existe fornecedor com este nome.");
            }
        }
        return fornecedorRepository.save(fornecedor);
    }

    @Transactional
    public void delete(Long id) {
        fornecedorRepository.deleteById(id);
    }
}
