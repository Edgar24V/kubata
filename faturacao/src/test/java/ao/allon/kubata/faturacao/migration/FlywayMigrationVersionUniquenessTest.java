package ao.allon.kubata.faturacao.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Detects duplicate versions in the module's Flyway SQL resource locations.
 */
class FlywayMigrationVersionUniquenessTest {

    private static final Pattern MIGRATION_NAME =
        Pattern.compile("^V([0-9]+)__[^/]+\\.sql$");

    @Test
    void sqliteMigrationVersionsMustBeUnique() throws IOException {
        assertUniqueVersions(Path.of("src/main/resources/db/migration/sqlite"));
    }

    @Test
    void postgresqlMigrationVersionsMustBeUnique() throws IOException {
        assertUniqueVersions(Path.of("src/main/resources/db/migration/postgresql"));
    }

    private void assertUniqueVersions(Path directory) throws IOException {
        assertTrue(Files.isDirectory(directory), "Migration directory not found: " + directory);
        Map<String, Path> versions = new HashMap<>();

        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                Matcher matcher = MIGRATION_NAME.matcher(path.getFileName().toString());
                if (!matcher.matches()) return;

                String version = matcher.group(1);
                Path previous = versions.putIfAbsent(version, path);
                assertTrue(previous == null,
                    () -> "Duplicate Flyway version V" + version + ": " + previous + " and " + path);
            });
        }
    }
}
