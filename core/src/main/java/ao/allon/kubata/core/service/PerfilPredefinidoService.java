package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.PerfilAcesso;
import ao.allon.kubata.core.domain.PermissaoPerfil;
import ao.allon.kubata.core.repository.PerfilAcessoRepository;
import ao.allon.kubata.core.repository.PermissaoPerfilRepository;
import ao.allon.kubata.core.security.PerfilPredefinidoCatalog;
import ao.allon.kubata.core.security.PerfilPredefinidoCatalog.PerfilPredefinido;
import ao.allon.kubata.core.security.PerfilPredefinidoCatalog.PermissaoDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Cria e mantém em dia os perfis de acesso pré-definidos definidos no
 * {@link PerfilPredefinidoCatalog}.
 *
 * A sincronização corre em transacções curtas e repete a transacção inteira
 * quando uma escrita concorrente (outra app a arrancar sobre a mesma base
 * SQLite) provoca SQLITE_BUSY/SQLITE_BUSY_SNAPSHOT. Uma falha definitiva é
 * registada no log sem derrubar o arranque da aplicação.
 */
@Service
public class PerfilPredefinidoService {

    private static final Logger logger = LoggerFactory.getLogger(PerfilPredefinidoService.class);

    private static final int MAX_SYNC_ATTEMPTS = 3;

    private final PerfilAcessoRepository perfilRepository;
    private final PermissaoPerfilRepository permissaoRepository;
    private final AcessoService acessoService;
    private final TransactionTemplate syncTransactionTemplate;

    public PerfilPredefinidoService(
            PerfilAcessoRepository perfilRepository,
            PermissaoPerfilRepository permissaoRepository,
            AcessoService acessoService,
            PlatformTransactionManager transactionManager
    ) {
        this.perfilRepository = perfilRepository;
        this.permissaoRepository = permissaoRepository;
        this.acessoService = acessoService;
        this.syncTransactionTemplate = new TransactionTemplate(transactionManager);
        this.syncTransactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
    }

    public void sincronizarPerfisPredefinidos() {
        for (int attempt = 1; attempt <= MAX_SYNC_ATTEMPTS; attempt++) {
            try {
                syncTransactionTemplate.execute(status -> {
                    sincronizarEmTransacao();
                    return null;
                });
                return;
            } catch (RuntimeException ex) {
                if (attempt >= MAX_SYNC_ATTEMPTS || !isRetryableSqliteLock(ex)) {
                    logger.error(
                            "Não foi possível sincronizar os perfis pré-definidos. "
                                    + "A aplicação arranca sem eles; serão repostos no próximo arranque.",
                            ex
                    );
                    return;
                }

                logger.warn(
                        "Bloqueio temporário SQLite ao sincronizar perfis pré-definidos "
                                + "(tentativa {}/{}). A transacção será reiniciada.",
                        attempt,
                        MAX_SYNC_ATTEMPTS
                );
                pauseBeforeRetry(attempt);
            }
        }
    }

    private void sincronizarEmTransacao() {
        int criados = 0;
        int atualizados = 0;

        for (PerfilPredefinido definicao : PerfilPredefinidoCatalog.todos()) {
            Optional<PerfilAcesso> existente = perfilRepository.findByCodigo(definicao.codigo());

            PerfilAcesso perfil;
            if (existente.isEmpty()) {
                perfil = PerfilAcesso.builder()
                        .codigo(definicao.codigo())
                        .descricao(definicao.descricao())
                        .observacoes(definicao.observacoes())
                        .sistema(true)
                        .activo(true)
                        .empresa(null)
                        .build();
                criados++;
            } else {
                perfil = existente.get();

                if (perfil.getEmpresa() != null) {
                    logger.warn(
                            "Perfil pré-definido {} ignorado: já existe um perfil de empresa "
                                    + "com o mesmo código.",
                            definicao.codigo()
                    );
                    continue;
                }
                if (!Boolean.TRUE.equals(perfil.getSistema())) {
                    logger.warn(
                            "Perfil pré-definido {} ignorado: já existe um perfil criado "
                                    + "manualmente com o mesmo código.",
                            definicao.codigo()
                    );
                    continue;
                }

                perfil.setDescricao(definicao.descricao());
                perfil.setObservacoes(definicao.observacoes());
                perfil.setSistema(true);
                perfil.setActivo(true);
                atualizados++;
            }

            acessoService.salvarPerfilComPermissoes(perfil, construirPermissoes(perfil, definicao));
        }

        logger.info(
                "Perfis pré-definidos sincronizados: {} criado(s), {} atualizado(s).",
                criados,
                atualizados
        );
    }

    private List<PermissaoPerfil> construirPermissoes(PerfilAcesso perfil, PerfilPredefinido definicao) {
        return definicao.permissoes().stream()
                .map(def -> construirPermissao(perfil, def))
                .toList();
    }

    private PermissaoPerfil construirPermissao(PerfilAcesso perfil, PermissaoDef def) {
        return PermissaoPerfil.builder()
                .perfil(perfil)
                .modulo(def.modulo())
                .recurso(def.recurso())
                .operacao(def.operacao())
                .permitido(true)
                .build();
    }

    private boolean isRetryableSqliteLock(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlException) {
                int primaryCode = sqlException.getErrorCode() & 0xff;
                if (primaryCode == 5 || primaryCode == 6) {
                    return true;
                }
            }

            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toUpperCase(Locale.ROOT);
                if (normalized.contains("SQLITE_BUSY")
                        || normalized.contains("SQLITE_LOCKED")
                        || normalized.contains("DATABASE IS LOCKED")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void pauseBeforeRetry(int attempt) {
        try {
            Thread.sleep(150L * attempt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "A sincronização de perfis pré-definidos foi interrompida durante a "
                            + "recuperação do bloqueio SQLite.",
                    ex
            );
        }
    }
}
