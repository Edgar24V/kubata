package ao.allon.kubata.admin.extensibilidade;

import javafx.scene.Node;
import javafx.scene.control.Label;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação de referência configurável para integrações externas.
 *
 * Representa o contrato administrativo mesmo quando o fornecedor externo
 * ainda não disponibilizou o SDK/plugin. Não inventa dados de negócio:
 * mantém apenas metadados e operações administrativas.
 */
public final class AplicacaoConfiguravel implements AplicacaoAdministrador {

    private final String nome;
    private final String abreviatura;
    private final List<String> roles;
    private final List<String> operacoes;
    private final List<String> entidadesLog;
    private final Map<String, String> logins = new ConcurrentHashMap<>();

    private final Audit audit = new Audit() {
        @Override
        public void registerSecurityPolicies() {
            // O fornecedor externo pode complementar este contrato ao instalar o plugin real.
        }

        @Override
        public boolean hasPermission(String userId, String operation) {
            return userId != null && operation != null;
        }

        @Override
        public List<String> getApplicationRoles() {
            return roles;
        }
    };

    private final ClsOperacoesAplicacao operacoesAplicacao = new ClsOperacoesAplicacao() {
        @Override
        public List<String> getOperacoesDisponiveis() {
            return operacoes;
        }

        @Override
        public String getDescricaoOperacao(String operacao) {
            return operacao == null ? "" : "Operação externa: " + operacao;
        }
    };

    private final ClsOperacoesLog operacoesLog = new ClsOperacoesLog() {
        @Override
        public List<String> getEntidadesLog() {
            return entidadesLog;
        }

        @Override
        public void registarLog(String entidade, String operacao, String detalhes) {
            // O contrato fica pronto para o plugin real encaminhar o log ao seu backend.
        }
    };

    private final ClsParametrizacoes parametrizacoes = new ClsParametrizacoes() {
        private Node formulario;

        @Override
        public Node getFormularioParametrizacao() {
            if (formulario == null) {
                Label label = new Label(
                        "A parametrização de " + nome +
                        " será fornecida pelo conector/plugin externo."
                );
                label.setWrapText(true);
                formulario = label;
            }
            return formulario;
        }

        @Override
        public void guardarParametrizacoes() {
            // Ponto de integração para o plugin real.
        }

        @Override
        public void carregarParametrizacoes() {
            // Ponto de integração para o plugin real.
        }
    };

    private final ClsServicos servicos = new ClsServicos() {
        private boolean ativo;

        @Override
        public void inicializarServicos() {
            ativo = true;
        }

        @Override
        public void encerrarServicos() {
            ativo = false;
        }

        @Override
        public Object executarServico(String nomeServico, Object... parametros) {
            if (!ativo) {
                throw new IllegalStateException("Os serviços de " + nome + " não estão inicializados.");
            }
            return Map.of(
                    "aplicacao", nome,
                    "servico", nomeServico,
                    "estado", "EXECUTADO"
            );
        }
    };

    private final ClsLoginsAssociados loginsAssociados = new ClsLoginsAssociados() {
        @Override
        public Map<String, String> getMapeamentoLogins() {
            return Map.copyOf(logins);
        }

        @Override
        public void associarLogin(String userId, String loginExterno) {
            if (userId == null || userId.isBlank()) {
                throw new IllegalArgumentException("Utilizador Kubata inválido.");
            }
            if (loginExterno == null || loginExterno.isBlank()) {
                throw new IllegalArgumentException("Login externo inválido.");
            }
            logins.put(userId, loginExterno);
        }

        @Override
        public void removerAssociacao(String userId) {
            if (userId != null) {
                logins.remove(userId);
            }
        }
    };

    public AplicacaoConfiguravel(
            String nome,
            String abreviatura,
            List<String> roles,
            List<String> operacoes,
            List<String> entidadesLog) {

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome da aplicação é obrigatório.");
        }
        if (abreviatura == null || !abreviatura.matches("[A-Za-z0-9]{3}")) {
            throw new IllegalArgumentException("A abreviatura deve conter exatamente 3 caracteres alfanuméricos.");
        }

        this.nome = nome.trim();
        this.abreviatura = abreviatura.toUpperCase();
        this.roles = List.copyOf(roles == null || roles.isEmpty()
                ? List.of("ADMIN", "USER", "VIEWER")
                : roles);
        this.operacoes = List.copyOf(operacoes == null || operacoes.isEmpty()
                ? List.of("CONSULTAR", "CRIAR", "EDITAR", "EXCLUIR")
                : operacoes);
        this.entidadesLog = List.copyOf(entidadesLog == null || entidadesLog.isEmpty()
                ? List.of("Aplicacao", "Configuracao", "Operacao")
                : entidadesLog);
    }

    public AplicacaoConfiguravel(String nome, String abreviatura) {
        this(nome, abreviatura, null, null, null);
    }

    @Override
    public String getNome() {
        return nome;
    }

    @Override
    public String getAbreviatura() {
        return abreviatura;
    }

    @Override
    public Audit getAudit() {
        return audit;
    }

    @Override
    public ClsOperacoesAplicacao getOperacoesAplicacao() {
        return operacoesAplicacao;
    }

    @Override
    public ClsOperacoesLog getOperacoesLog() {
        return operacoesLog;
    }

    @Override
    public ClsParametrizacoes getParametrizacoes() {
        return parametrizacoes;
    }

    @Override
    public ClsServicos getServicos() {
        return servicos;
    }

    @Override
    public ClsLoginsAssociados getLoginsAssociados() {
        return loginsAssociados;
    }
}
