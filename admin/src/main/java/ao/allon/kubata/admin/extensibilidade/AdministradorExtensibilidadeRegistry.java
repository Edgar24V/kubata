package ao.allon.kubata.admin.extensibilidade;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço responsável por gerir o registo de aplicações externas.
 * Valida abreviaturas (3 caracteres) e restringe abreviaturas reservadas do sistema.
 */
@Service
public class AdministradorExtensibilidadeRegistry {

    // Abreviaturas reservadas para módulos nativos (exemplo da documentação PRIMAVERA)
    private static final List<String> ABREVIATURAS_RESERVADAS = Arrays.asList("ADM", "CBL", "VND", "CMP", "INV");
    
    private final Map<String, AplicacaoAdministrador> aplicacoesRegistadas = new ConcurrentHashMap<>();
    private final Map<String, List<String>> empresasPorAplicacao = new ConcurrentHashMap<>();
    private final Map<String, List<String>> utilizadoresPorAplicacao = new ConcurrentHashMap<>();

    public AdministradorExtensibilidadeRegistry() {
        // Regista uma aplicação de exemplo para demonstração
        try {
            // Este registo normalmente seria feito por plugins externos
            // mas aqui fazemos para demonstrar a funcionalidade.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Regista uma nova aplicação no Administrator.
     * Corresponde à etapa de "Entradas no Registry".
     * @param aplicacao Implementação da aplicação externa
     * @throws IllegalArgumentException se a abreviatura for inválida ou reservada
     */
    public void registarAplicacao(AplicacaoAdministrador aplicacao) {
        String abrev = aplicacao.getAbreviatura();
        
        if (abrev == null || abrev.trim().length() != 3) {
            throw new IllegalArgumentException("A aplicação deve ter uma abreviatura única de 3 caracteres alfanuméricos.");
        }
        
        abrev = abrev.toUpperCase();
        
        if (ABREVIATURAS_RESERVADAS.contains(abrev)) {
            throw new IllegalArgumentException("A abreviatura " + abrev + " é reservada para módulos nativos do sistema.");
        }
        
        if (aplicacoesRegistadas.containsKey(abrev)) {
            throw new IllegalArgumentException("Já existe uma aplicação registada com a abreviatura " + abrev);
        }
        
        aplicacoesRegistadas.put(abrev, aplicacao);
        empresasPorAplicacao.put(abrev, new ArrayList<>());
        utilizadoresPorAplicacao.put(abrev, new ArrayList<>());
        
        // Inicializa o módulo no Administrator
        if (aplicacao.getAudit() != null) aplicacao.getAudit().registerSecurityPolicies();
        if (aplicacao.getServicos() != null) aplicacao.getServicos().inicializarServicos();
    }
    
    /**
     * Atribuição da aplicação a uma empresa.
     */
    public void atribuirAplicacaoAEmpresa(String abreviaturaAplicacao, String empresaId) {
        if (!aplicacoesRegistadas.containsKey(abreviaturaAplicacao.toUpperCase())) {
            throw new IllegalArgumentException("Aplicação não encontrada.");
        }
        List<String> empresas = empresasPorAplicacao.get(abreviaturaAplicacao.toUpperCase());
        if (!empresas.contains(empresaId)) {
            empresas.add(empresaId);
        }
    }
    
    /**
     * Atribuição da aplicação a um utilizador (Licenciamento/Acesso).
     */
    public void atribuirAplicacaoAUtilizador(String abreviaturaAplicacao, String utilizadorId) {
        if (!aplicacoesRegistadas.containsKey(abreviaturaAplicacao.toUpperCase())) {
            throw new IllegalArgumentException("Aplicação não encontrada.");
        }
        List<String> utilizadores = utilizadoresPorAplicacao.get(abreviaturaAplicacao.toUpperCase());
        if (!utilizadores.contains(utilizadorId)) {
            utilizadores.add(utilizadorId);
        }
    }
    
    /**
     * Retorna todas as aplicações registadas.
     */
    public List<AplicacaoAdministrador> getAplicacoesRegistadas() {
        return new ArrayList<>(aplicacoesRegistadas.values());
    }
}
