package ao.allon.kubata.financeiro.service;

import ao.allon.kubata.financeiro.domain.Caixa;
import ao.allon.kubata.financeiro.repository.CaixaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CaixaService {

    private final CaixaRepository caixaRepository;

    public CaixaService(CaixaRepository caixaRepository) {
        this.caixaRepository = caixaRepository;
    }

    public Caixa save(Caixa caixa) {
        return caixaRepository.save(caixa);
    }

    public Optional<Caixa> findById(Long id) {
        return caixaRepository.findById(id);
    }

    public Optional<Caixa> findPrincipal() {
        return caixaRepository.findByIsPrincipalTrue();
    }

    public List<Caixa> findAllAtivos() {
        return caixaRepository.findByIsAtivoTrue();
    }

    public List<Caixa> findAll() {
        return caixaRepository.findAll();
    }

    public void delete(Long id) {
        caixaRepository.deleteById(id);
    }

    public boolean existsByNome(String nome) {
        return caixaRepository.existsByNome(nome);
    }
}
