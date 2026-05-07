package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.core.domain.enums.ClasseConta;
import ao.allon.kubata.core.domain.enums.NaturezaConta;
import ao.allon.kubata.core.repository.LancamentoContabilRepository;
import ao.allon.kubata.core.repository.PlanoContaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PlanoContaService {

    private final PlanoContaRepository repository;
    private final LancamentoContabilRepository lancamentoRepository;

    public PlanoContaService(PlanoContaRepository repository, LancamentoContabilRepository lancamentoRepository) {
        this.repository = repository;
        this.lancamentoRepository = lancamentoRepository;
    }

    public List<PlanoConta> findAllRoots() {
        return repository.findByContaPaiIsNullOrderByCodigoAsc();
    }

    public List<PlanoConta> findAll() {
        return repository.findAll();
    }

    public List<PlanoConta> findByPai(PlanoConta pai) {
        return repository.findByContaPaiOrderByCodigoAsc(pai);
    }

    public Optional<PlanoConta> findByCodigo(String codigo) {
        return repository.findByCodigo(codigo);
    }

    @Transactional
    public PlanoConta save(PlanoConta conta) {
        if (conta.getId() == null && repository.existsByCodigo(conta.getCodigo())) {
            throw new IllegalArgumentException("Já existe uma conta com este código: " + conta.getCodigo());
        }

        // Validação hierárquica simples
        if (conta.getContaPai() != null) {
            if (!conta.getCodigo().startsWith(conta.getContaPai().getCodigo())) {
                throw new IllegalArgumentException("O código da subconta deve começar com o código da conta pai.");
            }
            conta.setNivel(conta.getContaPai().getNivel() + 1);
        } else {
            conta.setNivel(1);
        }

        return repository.save(conta);
    }

    @Transactional
    public void delete(Long id) {
        PlanoConta conta = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada."));

        if (conta.getSubContas() != null && !conta.getSubContas().isEmpty()) {
            throw new IllegalStateException("Não é possível excluir uma conta que possui subcontas.");
        }

        boolean temLancamentos = !lancamentoRepository.findByContaDebitoOrContaCredito(conta, conta).isEmpty();
        if (temLancamentos) {
            throw new IllegalStateException("Não é possível excluir uma conta que possui lançamentos contábeis.");
        }

        repository.delete(conta);
    }

    @Transactional
    public void importarPlanoContasCSV(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                
                String[] parts = line.split(";");
                if (parts.length < 5) continue;

                String codigo = parts[0].trim();
                String descricao = parts[1].trim();
                
                try {
                    ClasseConta classe = ClasseConta.valueOf(parts[2].trim());
                    NaturezaConta natureza = NaturezaConta.valueOf(parts[3].trim());
                    Boolean movimento = Boolean.parseBoolean(parts[4].trim());

                    if (!repository.existsByCodigo(codigo)) {
                        PlanoConta nova = new PlanoConta();
                        nova.setCodigo(codigo);
                        nova.setDescricao(descricao);
                        nova.setClasse(classe);
                        nova.setNatureza(natureza);
                        nova.setMovimento(movimento);
                        
                        // Tentar achar pai pelo código (simplificado)
                        if (codigo.length() > 1) {
                            String codigoPai = codigo.contains(".") ? 
                                    codigo.substring(0, codigo.lastIndexOf(".")) :
                                    codigo.substring(0, codigo.length() - 1);
                            repository.findByCodigo(codigoPai).ifPresent(nova::setContaPai);
                        }

                        save(nova);
                    }
                } catch (IllegalArgumentException e) {
                    // Log error and continue with next line
                    System.err.println("Erro ao importar linha: " + line + " - " + e.getMessage());
                }
            }
        }
    }
}
