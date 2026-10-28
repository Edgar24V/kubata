package ao.allon.kubata.core.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Localização da base de dados partilhada por todas as aplicações Kubata.
 *
 * Por predefinição é usado %USERPROFILE%/Kubata/kubata.db para que o Kubata
 * Administrator, Faturação e RH operem sobre a mesma base de dados — é isto
 * que faz utilizadores, perfis, permissões e o estado dos módulos criados no
 * Administrator reflectirem em todos os módulos. Pode ser substituído pela
 * variável de ambiente KUBATA_DB_PATH ou pela propriedade de sistema
 * kubata.db.path.
 */
public final class KubataDatabasePaths {

    public static final String ENV_VAR = "KUBATA_DB_PATH";
    public static final String SYS_PROP = "kubata.db.path";

    private KubataDatabasePaths() {
    }

    public static Path resolve() {
        String configured = System.getProperty(SYS_PROP);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(ENV_VAR);
        }
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured);
        }
        return Paths.get(System.getProperty("user.home"), "Kubata", "kubata.db");
    }

    public static String jdbcUrl() {
        return "jdbc:sqlite:" + resolve().toAbsolutePath() + "?journal_mode=WAL"
                + "&synchronous=NORMAL&busy_timeout=5000&foreign_keys=ON";
    }

    /**
     * Cria a pasta da base de dados partilhada. Deve ser chamado antes de o
     * Spring abrir a ligação SQLite.
     */
    public static void ensureDirectory() {
        try {
            Path parent = resolve().toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception e) {
            System.err.println("[KubataDatabasePaths] Não foi possível criar a pasta da base de dados: "
                    + e.getMessage());
        }
    }
}
