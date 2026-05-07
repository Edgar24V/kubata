package ao.allon.kubata.admin.extensibilidade;

import javafx.scene.Node;
import javafx.scene.control.Label;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Exemplo de implementação de uma aplicação externa (Ex: Contabilidade Externa).
 */
public class MockAplicacao implements AplicacaoAdministrador {

    @Override
    public String getNome() {
        return "Gestão de Ativos Fixos";
    }

    @Override
    public String getAbreviatura() {
        return "GAF";
    }

    @Override
    public Audit getAudit() {
        return new Audit() {
            @Override
            public void registerSecurityPolicies() {
                System.out.println("GAF: Registando políticas de segurança...");
            }

            @Override
            public boolean hasPermission(String userId, String operation) {
                return true;
            }

            @Override
            public List<String> getApplicationRoles() {
                return Arrays.asList("GAF_ADMIN", "GAF_USER", "GAF_VIEWER");
            }
        };
    }

    @Override
    public ClsOperacoesAplicacao getOperacoesAplicacao() {
        return new ClsOperacoesAplicacao() {
            @Override
            public List<String> getOperacoesDisponiveis() {
                return Arrays.asList("CRIAR_ATIVO", "DEPRECIAR", "BAIXA_ATIVO");
            }

            @Override
            public String getDescricaoOperacao(String operacao) {
                return "Operação de " + operacao + " no módulo de Ativos Fixos";
            }
        };
    }

    @Override
    public ClsOperacoesLog getOperacoesLog() {
        return new ClsOperacoesLog() {
            @Override
            public List<String> getEntidadesLog() {
                return Arrays.asList("AtivoFixo", "CategoriaAtivo", "MovimentoDepreciacao");
            }

            @Override
            public void registarLog(String entidade, String operacao, String detalhes) {
                System.out.println("GAF LOG: " + entidade + " | " + operacao + " | " + detalhes);
            }
        };
    }

    @Override
    public ClsParametrizacoes getParametrizacoes() {
        return new ClsParametrizacoes() {
            @Override
            public Node getFormularioParametrizacao() {
                return new Label("Formulário de Parametrizações dos Ativos Fixos (GAF)");
            }

            @Override
            public void guardarParametrizacoes() {
                System.out.println("GAF: Parametrizações guardadas.");
            }

            @Override
            public void carregarParametrizacoes() {
                System.out.println("GAF: Parametrizações carregadas.");
            }
        };
    }

    @Override
    public ClsServicos getServicos() {
        return new ClsServicos() {
            @Override
            public void inicializarServicos() {
                System.out.println("GAF: Serviços inicializados.");
            }

            @Override
            public void encerrarServicos() {
                System.out.println("GAF: Serviços encerrados.");
            }

            @Override
            public Object executarServico(String nomeServico, Object... parametros) {
                return "Serviço " + nomeServico + " executado com sucesso.";
            }
        };
    }

    @Override
    public ClsLoginsAssociados getLoginsAssociados() {
        return new ClsLoginsAssociados() {
            private Map<String, String> logins = new HashMap<>();
            @Override
            public Map<String, String> getMapeamentoLogins() {
                return logins;
            }

            @Override
            public void associarLogin(String userId, String loginExterno) {
                logins.put(userId, loginExterno);
            }

            @Override
            public void removerAssociacao(String userId) {
                logins.remove(userId);
            }
        };
    }
}
