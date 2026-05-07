package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Empresa;
import ao.allon.kubata.faturacao.repository.FaturacaoEmpresaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmpresaService {

    private final FaturacaoEmpresaRepository empresaRepository;

    @Value("${kubata.empresa.nome:Minha Empresa}")
    private String defaultNome = "Minha Empresa";

    @Value("${kubata.empresa.nif:999999999}")
    private String defaultNif = "999999999";

    @Value("${kubata.empresa.endereco:Endereço da Empresa}")
    private String defaultEndereco = "Endereço da Empresa";

    @Value("${kubata.empresa.cidade:Luanda}")
    private String defaultCidade = "Luanda";

    public EmpresaService(FaturacaoEmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    @Transactional(readOnly = true)
    public Empresa getDadosEmpresa() {
        Empresa empresa = empresaRepository.findFirstByOrderByIdAsc(); // Método customizado para pegar o primeiro registro
        if (empresa == null) {
            // Retorna uma instância vazia ou padrão se não existir
            empresa = new Empresa();
            empresa.setNome(defaultNome);
            empresa.setNif(defaultNif);
            empresa.setEndereco(defaultEndereco);
            empresa.setCidade(defaultCidade);
        }
        return empresa;
    }

    @Transactional
    public Empresa salvarDadosEmpresa(Empresa empresa) {
        // Garante que só existe uma empresa (ou atualiza a existente)
        Empresa existing = empresaRepository.findFirstByOrderByIdAsc();
        if (existing != null && (empresa.getId() == null || !empresa.getId().equals(existing.getId()))) {
            empresa.setId(existing.getId());
        }
        return empresaRepository.save(empresa);
    }
}
