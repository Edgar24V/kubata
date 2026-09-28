package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.ExercicioFiscal;
import ao.allon.kubata.core.repository.ExercicioFiscalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExercicioFiscalService {

    private final ExercicioFiscalRepository repository;

    @Transactional
    public ExercicioFiscal criarExercicio(Empresa empresa, int ano) {
        if (empresa == null || empresa.getId() == null) {
            throw new IllegalArgumentException("A empresa é obrigatória para criar um exercício.");
        }

        if (ano < 2000 || ano > 2100) {
            throw new IllegalArgumentException("O ano do exercício deve estar entre 2000 e 2100.");
        }

        if (repository.findByEmpresaIdAndAno(empresa.getId(), ano).isPresent()) {
            throw new IllegalStateException("O exercício para o ano " + ano + " já existe para esta empresa.");
        }

        ExercicioFiscal exercicio = ExercicioFiscal.builder()
                .empresa(empresa)
                .ano(ano)
                .dataInicio(LocalDate.of(ano, 1, 1))
                .dataFim(LocalDate.of(ano, 12, 31))
                .estado(ExercicioFiscal.EstadoExercicio.ABERTO)
                .build();

        return repository.save(exercicio);
    }

    @Transactional(readOnly = true)
    public List<ExercicioFiscal> listarPorEmpresa(Long empresaId) {
        return repository.findByEmpresaIdOrderByAnoDesc(empresaId);
    }

    @Transactional(readOnly = true)
    public List<ExercicioFiscal> listarTodosComEmpresa() {
        return repository.findAllWithEmpresaOrderByEmpresaNomeAndAnoDesc();
    }

    @Transactional(readOnly = true)
    public Optional<ExercicioFiscal> buscarPorAno(Long empresaId, int ano) {
        return repository.findByEmpresaIdAndAno(empresaId, ano);
    }

    @Transactional
    public ExercicioFiscal iniciarEncerramento(Long exercicioId) {
        ExercicioFiscal exercicio = getRequired(exercicioId);

        if (exercicio.getEstado() != ExercicioFiscal.EstadoExercicio.ABERTO) {
            throw new IllegalStateException(
                    "Só é possível iniciar o encerramento de um exercício que esteja Aberto."
            );
        }

        exercicio.setEstado(ExercicioFiscal.EstadoExercicio.ENCERRAMENTO);
        return repository.save(exercicio);
    }

    @Transactional
    public ExercicioFiscal fecharExercicio(Long exercicioId, String utilizador) {
        ExercicioFiscal exercicio = getRequired(exercicioId);

        if (exercicio.getEstado() != ExercicioFiscal.EstadoExercicio.ENCERRAMENTO) {
            throw new IllegalStateException(
                    "O exercício deve estar em 'Em Encerramento' antes de ser fechado."
            );
        }

        exercicio.setEstado(ExercicioFiscal.EstadoExercicio.FECHADO);
        exercicio.setEncerradoEm(LocalDateTime.now());
        exercicio.setEncerradoPor(normalizeUser(utilizador));
        return repository.save(exercicio);
    }

    @Transactional
    public ExercicioFiscal reabrirExercicio(Long exercicioId) {
        ExercicioFiscal exercicio = getRequired(exercicioId);

        if (exercicio.getEstado() != ExercicioFiscal.EstadoExercicio.FECHADO) {
            throw new IllegalStateException("Só é possível reabrir um exercício fechado.");
        }

        exercicio.setEstado(ExercicioFiscal.EstadoExercicio.ABERTO);
        exercicio.setEncerradoEm(null);
        exercicio.setEncerradoPor(null);
        return repository.save(exercicio);
    }

    @Transactional
    public ExercicioFiscal definirComoActual(Long exercicioId) {
        ExercicioFiscal exercicio = getRequired(exercicioId);

        if (exercicio.getEstado() == ExercicioFiscal.EstadoExercicio.FECHADO) {
            throw new IllegalStateException("Um exercício fechado não pode ser definido como actual.");
        }

        Empresa empresa = exercicio.getEmpresa();
        empresa.setExercicioActual(exercicio.getAno());
        return repository.save(exercicio);
    }

    private ExercicioFiscal getRequired(Long exercicioId) {
        if (exercicioId == null) {
            throw new IllegalArgumentException("O exercício é obrigatório.");
        }

        return repository.findById(exercicioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "O exercício selecionado já não existe."
                ));
    }

    private String normalizeUser(String utilizador) {
        return utilizador == null || utilizador.isBlank()
                ? "Sistema"
                : utilizador.trim();
    }
}
