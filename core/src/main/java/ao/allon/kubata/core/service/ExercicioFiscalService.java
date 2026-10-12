package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.ExercicioFiscal;
import ao.allon.kubata.core.domain.SerieDocumento;
import ao.allon.kubata.core.repository.ExercicioFiscalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gestão do ciclo de vida dos exercícios fiscais.
 *
 * <p>O assistente trabalha com o exercício imediatamente anterior como referência
 * e replica somente configuração documental segura (séries). Nunca duplica
 * documentos, movimentos, existências, lançamentos ou saldos.</p>
 */
@Service
@RequiredArgsConstructor
public class ExercicioFiscalService {

    private static final int ANO_MINIMO = 2000;
    private static final int ANO_MAXIMO = 2100;

    /**
     * Códigos suportados pelo assistente para preparar séries documentais.
     * O valor é o nome de área usado por TipoDocumentoSAFT.
     */
    private static final Map<String, String> MODULOS_COM_SERIES = Map.of(
            "VENDAS", "Vendas",
            "COMPRAS", "Compras",
            "INVENTARIO", "Stock",
            "FINANCEIRO", "Financeiro"
    );

    private final ExercicioFiscalRepository repository;
    private final SerieDocumentoService serieDocumentoService;

    /**
     * Entrada retrocompatível para criação simples usada por processos de
     * provisionamento. A criação interactiva deve usar criarExercicioAssistido.
     */
    @Transactional
    public ExercicioFiscal criarExercicio(Empresa empresa, int ano) {
        validarEmpresa(empresa);
        validarAno(ano);

        if (repository.findByEmpresaIdAndAno(empresa.getId(), ano).isPresent()) {
            throw new IllegalStateException(
                    "O exercício para o ano " + ano + " já existe para esta empresa."
            );
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

    /**
     * Valida e finaliza o plano criado pelo assistente.
     *
     * @param empresa empresa escolhida no assistente
     * @param anoBase ano de origem, obrigatório ser imediatamente anterior ao destino
     * @param anoDestino ano fiscal novo
     * @param modulos códigos: VENDAS, COMPRAS, INVENTARIO e FINANCEIRO
     * @param definirComoActual indica se o novo período será o exercício activo da empresa
     * @return resultado com registo criado, séries preparadas e alertas não bloqueantes
     */
    @Transactional
    public ResultadoCriacao criarExercicioAssistido(
            Empresa empresa,
            int anoBase,
            int anoDestino,
            Set<String> modulos,
            boolean definirComoActual
    ) {
        validarEmpresa(empresa);
        validarAno(anoDestino);

        if (anoBase != anoDestino - 1) {
            throw new IllegalArgumentException(
                    "A base deve ser o exercício imediatamente anterior ao destino. "
                            + "Para " + anoDestino + ", seleccione " + (anoDestino - 1) + "."
            );
        }

        if (anoBase < ANO_MINIMO || anoBase > ANO_MAXIMO) {
            throw new IllegalArgumentException("O ano-base deve estar entre 2000 e 2100.");
        }

        ExercicioFiscal base = repository.findByEmpresaIdAndAno(empresa.getId(), anoBase)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Não existe o exercício-base " + anoBase + " para a empresa "
                                + safe(empresa.getNome()) + ". Crie ou importe primeiro esse exercício."
                ));

        if (base.getEstado() == ExercicioFiscal.EstadoExercicio.FUTURO) {
            throw new IllegalStateException(
                    "O exercício-base " + anoBase + " está marcado como Futuro e não pode ser usado."
            );
        }

        if (repository.findByEmpresaIdAndAno(empresa.getId(), anoDestino).isPresent()) {
            throw new IllegalStateException(
                    "O exercício " + anoDestino + " já existe para esta empresa. "
                            + "Actualize a lista antes de tentar novamente."
            );
        }

        Set<String> modulosNormalizados = normalizarModulos(modulos);
        Empresa empresaGerida = base.getEmpresa() == null ? empresa : base.getEmpresa();

        List<String> avisos = new ArrayList<>();
        if (base.getEstado() != ExercicioFiscal.EstadoExercicio.FECHADO) {
            avisos.add(
                    "O exercício-base " + anoBase + " ainda não está fechado. "
                            + "Confirme que os saldos e operações do período anterior estão revistos."
            );
        }

        List<SerieDocumento> seriesBase = serieDocumentoService.listarPorEmpresa(empresa.getId())
                .stream()
                .filter(serie -> serie.getExercicio() != null
                        && serie.getExercicio() == anoBase)
                .filter(serie -> serie.getTipoDocumento() != null)
                .filter(serie -> modulosNormalizados.contains(
                        moduloPorArea(serie.getTipoDocumento().getArea())
                ))
                .toList();

        Set<String> modulosComSeries = seriesBase.stream()
                .map(serie -> moduloPorArea(serie.getTipoDocumento().getArea()))
                .filter(modulo -> !modulo.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (String modulo : modulosNormalizados) {
            if (!modulosComSeries.contains(modulo)) {
                avisos.add(
                        "Não há séries documentais de " + nomeModulo(modulo)
                                + " no exercício-base; o período será criado sem copiar séries desse módulo."
                );
            }
        }

        ExercicioFiscal novo = ExercicioFiscal.builder()
                .empresa(empresaGerida)
                .ano(anoDestino)
                .dataInicio(LocalDate.of(anoDestino, 1, 1))
                .dataFim(LocalDate.of(anoDestino, 12, 31))
                .estado(ExercicioFiscal.EstadoExercicio.ABERTO)
                .observacoes("Preparação pelo Assistente de Exercícios do Kubata.")
                .build();

        novo = repository.save(novo);

        List<SerieDocumento> seriesCriadas = seriesBase.isEmpty()
                ? List.of()
                : serieDocumentoService.replicarParaExercicio(
                        empresaGerida, seriesBase, anoDestino
                );

        if (definirComoActual) {
            empresaGerida.setExercicioActual(anoDestino);
        }

        String resumo = "Preparado pelo Assistente Kubata | Base: " + anoBase
                + " | Módulos: "
                + (modulosNormalizados.isEmpty()
                    ? "nenhum"
                    : String.join(", ", modulosNormalizados))
                + " | Séries criadas: " + seriesCriadas.size() + ".";
        if (!avisos.isEmpty()) {
            resumo += " Avisos: " + avisos.size() + ".";
        }
        novo.setObservacoes(resumo.length() > 500 ? resumo.substring(0, 500) : resumo);
        novo = repository.save(novo);

        return new ResultadoCriacao(
                novo,
                anoBase,
                seriesCriadas.size(),
                Set.copyOf(modulosNormalizados),
                List.copyOf(avisos),
                definirComoActual
        );
    }

    private Set<String> normalizarModulos(Set<String> modulos) {
        Set<String> normalizados = new LinkedHashSet<>();
        if (modulos == null) {
            return normalizados;
        }

        for (String modulo : modulos) {
            if (modulo == null || modulo.isBlank()) {
                continue;
            }
            String key = modulo.trim().toUpperCase(Locale.ROOT);
            if (!MODULOS_COM_SERIES.containsKey(key)) {
                throw new IllegalArgumentException(
                        "O módulo '" + modulo + "' não é suportado pelo assistente de séries fiscais."
                );
            }
            normalizados.add(key);
        }
        return normalizados;
    }

    private String moduloPorArea(String area) {
        if (area == null) {
            return "";
        }
        return MODULOS_COM_SERIES.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(area.trim()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("");
    }

    private String nomeModulo(String codigo) {
        return switch (codigo) {
            case "VENDAS" -> "Vendas";
            case "COMPRAS" -> "Compras";
            case "INVENTARIO" -> "Inventário/Stock";
            case "FINANCEIRO" -> "Financeiro";
            default -> codigo;
        };
    }

    private void validarEmpresa(Empresa empresa) {
        if (empresa == null || empresa.getId() == null) {
            throw new IllegalArgumentException(
                    "A empresa é obrigatória e tem de estar previamente gravada."
            );
        }
        if (!empresa.getAtiva()) {
            throw new IllegalArgumentException(
                    "A empresa está inactiva. Active-a antes de criar um exercício fiscal."
            );
        }
    }

    private void validarAno(int ano) {
        if (ano < ANO_MINIMO || ano > ANO_MAXIMO) {
            throw new IllegalArgumentException(
                    "O ano do exercício deve estar entre " + ANO_MINIMO + " e " + ANO_MAXIMO + "."
            );
        }
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

    private String safe(String value) {
        return value == null || value.isBlank() ? "empresa seleccionada" : value;
    }

    public record ResultadoCriacao(
            ExercicioFiscal exercicio,
            int anoBase,
            int seriesCriadas,
            Set<String> modulosPreparados,
            List<String> avisos,
            boolean definidoComoActual
    ) {}
}
