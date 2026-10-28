package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.SessionTicket;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.security.PermissionCatalog;
import ao.allon.kubata.core.service.SessionTicketService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Lança as aplicações Kubata que correm num JVM separado (Faturação, RH) com um bilhete de
 * sessão, evitando o segundo pedido de credenciais.
 *
 * <p>O comando é configurável: cada aplicação tem override {@code kubata.apps.<CODE>.command}
 * (tokens separados por ponto e vírgula) e todos aceitam os marcadores {@code {module}},
 * {@code {ticket}} e {@code {code}}.</p>
 */
@Service
public class ApplicationLauncherService {

    /** Aplicações que existem como processos próprios; as restantes abrem dentro do hub. */
    private static final Set<String> APLICACOES_PROCESSO = Set.of(PermissionCatalog.FATURACAO, PermissionCatalog.RH);

    private static final String MARCADOR_BILHETE = "{ticket}";
    private static final DateTimeFormatter CARIMBO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final Map<String, List<String>> COMANDOS_POR_DEFECTO = Map.of(
            PermissionCatalog.FATURACAO, List.of(
                    "mvn.cmd", "-o", "-pl", "faturacao", "spring-boot:run",
                    "-Dspring-boot.run.arguments=--kubata-launch-ticket=" + MARCADOR_BILHETE),
            PermissionCatalog.RH, List.of(
                    "mvn.cmd", "-o", "-pl", "rh", "spring-boot:run",
                    "-Dspring-boot.run.arguments=--kubata-launch-ticket=" + MARCADOR_BILHETE)
    );

    private final SessionTicketService ticketService;
    private final SessionManager sessionManager;
    private final EmpresaRepository empresaRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "kubata-admin-launcher");
        thread.setDaemon(true);
        return thread;
    });

    @Value("${kubata.apps.FATURACAO.command:}")
    private String faturacaoCommand;

    @Value("${kubata.apps.RH.command:}")
    private String rhCommand;

    @Value("${kubata.apps.working-dir:${user.dir}}")
    private String workingDir;

    @Value("${kubata.apps.log-dir:${user.home}/Kubata/logs}")
    private String logDir;

    public ApplicationLauncherService(SessionTicketService ticketService,
                                      SessionManager sessionManager,
                                      EmpresaRepository empresaRepository) {
        this.ticketService = ticketService;
        this.sessionManager = sessionManager;
        this.empresaRepository = empresaRepository;
    }

    public boolean isLaunchable(String applicationCode) {
        return APLICACOES_PROCESSO.contains(normalizar(applicationCode));
    }

    /**
     * Um código de catálogo legado ("BILLING", "HR") e um código de aplicação ("faturacao")
     * têm de apontar para a mesma aplicação.
     */
    private String normalizar(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        String comoAplicacao = PermissionCatalog.permissionModuleForApplication(codigo);
        if (APLICACOES_PROCESSO.contains(comoAplicacao)) {
            return comoAplicacao;
        }
        String comoCatalogo = PermissionCatalog.permissionModuleForCatalog(codigo);
        return APLICACOES_PROCESSO.contains(comoCatalogo) ? comoCatalogo : comoAplicacao;
    }

    /** Emite o bilhete e lança o processo fora da thread FX; {@code onStatus} recebe o progresso. */
    public void abrirAsync(String applicationCode, Consumer<String> onStatus) {
        String codigo = normalizar(applicationCode);
        if (!isLaunchable(codigo)) {
            onStatus.accept("Aplicação '" + applicationCode + "' abre dentro do hub, não como processo separado.");
            return;
        }

        User emissor = sessionManager.getUser();
        if (emissor == null) {
            onStatus.accept("Sem sessão activa no Administrator.");
            return;
        }

        Empresa empresa = empresaRepository.findFirstByPredefinidaTrueAndAtivaTrue()
                .orElse(null);

        executor.submit(() -> {
            try {
                SessionTicket ticket = ticketService.emitir(
                        emissor,
                        codigo,
                        empresa == null ? null : empresa.getId(),
                        empresa == null ? null : empresa.getExercicioActual());

                onStatus.accept("A lançar " + codigo + "...");
                Process process = iniciar(codigo, ticket.getToken());
                onStatus.accept(codigo + " lançada (PID " + process.pid() + ").");
            } catch (Exception ex) {
                onStatus.accept("Falha ao lançar " + codigo + ": " + mensagem(ex));
            }
        });
    }

    private Process iniciar(String codigo, String token) throws Exception {
        List<String> comando = montarComando(codigo, token);
        Path diretorio = Path.of(workingDir);
        File log = ficheiroLog(codigo);

        ProcessBuilder builder = new ProcessBuilder(comando);
        builder.directory(diretorio.toFile());
        builder.redirectErrorStream(true);
        if (log != null) {
            builder.redirectOutput(log);
        }
        return builder.start();
    }

    private List<String> montarComando(String codigo, String uuid) {
        List<String> tokens = comandoConfigurado(codigo);
        String modulo = codigo.toLowerCase(Locale.ROOT);
        if ("RH".equals(codigo)) {
            modulo = "rh";
        }

        List<String> comando = new ArrayList<>(tokens.size() + 2);
        for (String parte : tokens) {
            comando.add(parte.replace(MARCADOR_BILHETE, uuid)
                    .replace("{module}", modulo)
                    .replace("{code}", codigo));
        }

        // CreateProcess não executa .bat/.cmd: é preciso passar pelo cmd.exe.
        String executavel = comando.get(0).toLowerCase(Locale.ROOT);
        if (isWindows() && (executavel.endsWith(".cmd") || executavel.endsWith(".bat"))) {
            List<String> embrulhado = new ArrayList<>();
            embrulhado.add(System.getenv().getOrDefault("ComSpec", "cmd.exe"));
            embrulhado.add("/c");
            embrulhado.addAll(comando);
            return embrulhado;
        }
        return comando;
    }

    private List<String> comandoConfigurado(String codigo) {
        String configurado = PermissionCatalog.FATURACAO.equals(codigo) ? faturacaoCommand : rhCommand;
        if (configurado == null || configurado.isBlank()) {
            return COMANDOS_POR_DEFECTO.get(codigo);
        }
        List<String> tokens = new ArrayList<>();
        for (String parte : configurado.split(";")) {
            String token = parte.trim();
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        if (tokens.isEmpty()) {
            return COMANDOS_POR_DEFECTO.get(codigo);
        }
        return tokens;
    }

    private File ficheiroLog(String codigo) {
        try {
            Path pasta = Path.of(logDir);
            Files.createDirectories(pasta);
            return pasta.resolve(codigo.toLowerCase(Locale.ROOT) + "-launch-"
                    + LocalDateTime.now().format(CARIMBO) + ".log").toFile();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private String mensagem(Exception ex) {
        String mensagem = ex.getMessage();
        return mensagem == null || mensagem.isBlank() ? ex.getClass().getSimpleName() : mensagem;
    }
}
