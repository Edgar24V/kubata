package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.ExercicioFiscal;
import ao.allon.kubata.core.repository.ExercicioFiscalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExercicioFiscalService {

    private final ExercicioFiscalRepository repository;

    @Transactional
    public ExercicioFiscal criarExercicio(Empresa empresa, int ano) {
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

    public List<ExercicioFiscal> listarPorEmpresa(Long empresaId) {
        return repository.findByEmpresaIdOrderByAnoDesc(empresaId);
    }

    public Optional<ExercicioFiscal> buscarPorAno(Long empresaId, int ano) {
        return repository.findByEmpresaIdAndAno(empresaId, ano);
    }
}
