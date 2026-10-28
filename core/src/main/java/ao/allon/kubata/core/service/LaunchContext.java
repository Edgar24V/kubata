package ao.allon.kubata.core.service;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Contexto de trabalho (empresa + exercício) transmitido pelo Kubata Administrator no
 * lançamento de uma aplicação. Persiste apenas enquanto o processo decorre.
 */
@Component
public class LaunchContext {

    private volatile Long empresaId;
    private volatile Integer exercicioAno;

    public void set(Long empresaId, Integer exercicioAno) {
        this.empresaId = empresaId;
        this.exercicioAno = exercicioAno;
    }

    public Optional<Long> empresaId() {
        return Optional.ofNullable(empresaId);
    }

    public Optional<Integer> exercicioAno() {
        return Optional.ofNullable(exercicioAno);
    }

    public void clear() {
        this.empresaId = null;
        this.exercicioAno = null;
    }
}
