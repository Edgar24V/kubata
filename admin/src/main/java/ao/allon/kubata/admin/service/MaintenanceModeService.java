package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.ParametroSistema;
import ao.allon.kubata.core.repository.ParametroSistemaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Estado global de manutenção guardado na parametrização do sistema.
 * Desta forma o modo de manutenção sobrevive ao reinício e pode ser
 * consultado por outros componentes do Kubata.
 */
@Service
public class MaintenanceModeService {

    public static final String KEY_ENABLED = "SISTEMA.MANUTENCAO.ATIVO";
    public static final String KEY_REASON = "SISTEMA.MANUTENCAO.MOTIVO";
    public static final String KEY_SINCE = "SISTEMA.MANUTENCAO.DESDE";

    private final ParametroSistemaRepository repository;

    public MaintenanceModeService(ParametroSistemaRepository repository) {
        this.repository = repository;
    }

    public boolean isEnabled() {
        return Boolean.parseBoolean(read(KEY_ENABLED, "false"));
    }

    public String getReason() {
        return read(KEY_REASON, "Manutenção programada.");
    }

    public LocalDateTime getSince() {
        String value = read(KEY_SINCE, "");
        if (value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void enable(String reason, String user) {
        LocalDateTime now = LocalDateTime.now();
        write(KEY_ENABLED, "true", "BOOLEAN", "Modo de manutenção global.", user);
        write(KEY_REASON,
                reason == null || reason.isBlank() ? "Manutenção programada." : reason.trim(),
                "STRING",
                "Motivo apresentado durante a manutenção.",
                user);
        write(KEY_SINCE, now.toString(), "STRING",
                "Data/hora de início do modo de manutenção.",
                user);
    }

    public void disable(String user) {
        write(KEY_ENABLED, "false", "BOOLEAN", "Modo de manutenção global.", user);
        write(KEY_REASON, "", "STRING",
                "Motivo apresentado durante a manutenção.",
                user);
        write(KEY_SINCE, "", "STRING",
                "Data/hora de início do modo de manutenção.",
                user);
    }

    private String read(String key, String fallback) {
        try {
            return repository.findByChaveAndEmpresaIdIsNull(key)
                    .map(ParametroSistema::getValor)
                    .filter(v -> v != null && !v.isBlank())
                    .orElse(fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void write(String key,
                       String value,
                       String type,
                       String description,
                       String user) {

        ParametroSistema p = repository
                .findByChaveAndEmpresaIdIsNull(key)
                .orElseGet(ParametroSistema::new);

        p.setEmpresa(null);
        p.setChave(key);
        p.setValor(value);
        p.setTipoValor(type);
        p.setDescricao(description);
        p.setGrupo("SISTEMA");
        p.setEditavel(true);
        p.setAtualizadoEm(LocalDateTime.now());
        p.setAtualizadoPor(user == null || user.isBlank() ? "Sistema" : user);

        repository.save(p);
    }
}
