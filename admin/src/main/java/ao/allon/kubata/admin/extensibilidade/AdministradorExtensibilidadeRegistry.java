package ao.allon.kubata.admin.extensibilidade;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry de aplicações externas do Kubata Administrator.
 *
 * As integrações são mantidas em memória e os seus metadados são persistidos
 * localmente para que o catálogo sobreviva ao reinício da aplicação.
 */
@Service
public class AdministradorExtensibilidadeRegistry {

    private static final List<String> ABREVIATURAS_RESERVADAS =
            Arrays.asList("ADM", "CBL", "VND", "CMP", "INV");

    private static final String PREF_NODE =
            "ao.allon.kubata.admin.extensibilidade";
    private static final String PREF_PREFIX = "APP_";

    private final Map<String, AplicacaoAdministrador> aplicacoesRegistadas =
            new ConcurrentHashMap<>();
    private final Map<String, List<String>> empresasPorAplicacao =
            new ConcurrentHashMap<>();
    private final Map<String, List<String>> utilizadoresPorAplicacao =
            new ConcurrentHashMap<>();

    private final Preferences preferences =
            Preferences.userRoot().node(PREF_NODE);

    public AdministradorExtensibilidadeRegistry() {
        carregarRegistosPersistidos();
    }

    public synchronized void registarAplicacao(AplicacaoAdministrador aplicacao) {
        if (aplicacao == null) {
            throw new IllegalArgumentException("A aplicação não pode ser nula.");
        }

        String nome = aplicacao.getNome();
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome da aplicação é obrigatório.");
        }

        String abrev = normalizarAbreviatura(aplicacao.getAbreviatura());

        if (ABREVIATURAS_RESERVADAS.contains(abrev)) {
            throw new IllegalArgumentException(
                    "A abreviatura " + abrev + " é reservada para módulos nativos do sistema.");
        }

        if (aplicacoesRegistadas.containsKey(abrev)) {
            throw new IllegalArgumentException(
                    "Já existe uma aplicação registada com a abreviatura " + abrev);
        }

        aplicacoesRegistadas.put(abrev, aplicacao);
        empresasPorAplicacao.putIfAbsent(abrev, new ArrayList<>());
        utilizadoresPorAplicacao.putIfAbsent(abrev, new ArrayList<>());

        preferences.put(PREF_PREFIX + abrev, nome.trim());

        if (aplicacao.getAudit() != null) {
            aplicacao.getAudit().registerSecurityPolicies();
        }
        if (aplicacao.getServicos() != null) {
            aplicacao.getServicos().inicializarServicos();
        }
    }

    public synchronized void removerAplicacao(String abreviaturaAplicacao) {
        String abrev = normalizarAbreviatura(abreviaturaAplicacao);

        AplicacaoAdministrador aplicacao = aplicacoesRegistadas.remove(abrev);
        if (aplicacao == null) {
            throw new IllegalArgumentException("Aplicação não encontrada: " + abrev);
        }

        if (aplicacao.getServicos() != null) {
            aplicacao.getServicos().encerrarServicos();
        }

        empresasPorAplicacao.remove(abrev);
        utilizadoresPorAplicacao.remove(abrev);
        preferences.remove(PREF_PREFIX + abrev);
    }

    public void atribuirAplicacaoAEmpresa(String abreviaturaAplicacao, String empresaId) {
        String abrev = normalizarAbreviatura(abreviaturaAplicacao);
        if (!aplicacoesRegistadas.containsKey(abrev)) {
            throw new IllegalArgumentException("Aplicação não encontrada.");
        }
        if (empresaId == null || empresaId.isBlank()) {
            throw new IllegalArgumentException("Empresa inválida.");
        }

        List<String> empresas = empresasPorAplicacao.get(abrev);
        if (!empresas.contains(empresaId)) {
            empresas.add(empresaId);
        }
    }

    public void atribuirAplicacaoAUtilizador(String abreviaturaAplicacao, String utilizadorId) {
        String abrev = normalizarAbreviatura(abreviaturaAplicacao);
        if (!aplicacoesRegistadas.containsKey(abrev)) {
            throw new IllegalArgumentException("Aplicação não encontrada.");
        }
        if (utilizadorId == null || utilizadorId.isBlank()) {
            throw new IllegalArgumentException("Utilizador inválido.");
        }

        List<String> utilizadores = utilizadoresPorAplicacao.get(abrev);
        if (!utilizadores.contains(utilizadorId)) {
            utilizadores.add(utilizadorId);
        }
    }

    public List<AplicacaoAdministrador> getAplicacoesRegistadas() {
        return aplicacoesRegistadas.values().stream()
                .sorted(java.util.Comparator.comparing(
                        AplicacaoAdministrador::getNome,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void carregarRegistosPersistidos() {
        try {
            for (String key : preferences.keys()) {
                if (!key.startsWith(PREF_PREFIX)) {
                    continue;
                }

                String abrev = key.substring(PREF_PREFIX.length()).toUpperCase();
                String nome = preferences.get(key, null);

                if (nome == null || nome.isBlank()) {
                    continue;
                }

                AplicacaoAdministrador app =
                        new AplicacaoConfiguravel(nome, abrev);

                aplicacoesRegistadas.put(abrev, app);
                empresasPorAplicacao.put(abrev, new ArrayList<>());
                utilizadoresPorAplicacao.put(abrev, new ArrayList<>());

                if (app.getAudit() != null) {
                    app.getAudit().registerSecurityPolicies();
                }
                if (app.getServicos() != null) {
                    app.getServicos().inicializarServicos();
                }
            }
        } catch (Exception ignored) {
            // O catálogo não pode impedir o arranque do Administrator.
        }
    }

    private String normalizarAbreviatura(String abrev) {
        if (abrev == null || !abrev.trim().matches("[A-Za-z0-9]{3}")) {
            throw new IllegalArgumentException(
                    "A aplicação deve ter uma abreviatura única de 3 caracteres alfanuméricos.");
        }
        return abrev.trim().toUpperCase();
    }
}
