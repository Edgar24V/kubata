package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.ExercicioFiscal;
import ao.allon.kubata.core.domain.ParametroSistema;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.ExercicioFiscalRepository;
import ao.allon.kubata.core.repository.ParametroSistemaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provisionamento central de uma empresa no ecossistema Kubata.
 *
 * <p>O Administrator é a fonte de verdade para a configuração transversal:
 * empresa, exercício fiscal, parâmetros por empresa e estado inicial do setup.</p>
 */
@Service
public class EmpresaSetupService {

    private final EmpresaRepository empresaRepository;
    private final ExercicioFiscalRepository exercicioFiscalRepository;
    private final ParametroSistemaRepository parametroSistemaRepository;

    public EmpresaSetupService(EmpresaRepository empresaRepository,
                               ExercicioFiscalRepository exercicioFiscalRepository,
                               ParametroSistemaRepository parametroSistemaRepository) {
        this.empresaRepository = empresaRepository;
        this.exercicioFiscalRepository = exercicioFiscalRepository;
        this.parametroSistemaRepository = parametroSistemaRepository;
    }

    @Transactional
    public Empresa saveAndProvision(Empresa empresa,
                                     boolean abrirExercicio,
                                     boolean backupEnabled,
                                     String backupFrequency,
                                     int backupRetentionDays,
                                     boolean mfaAdminRequired,
                                     Map<String, String> parameters,
                                     String updatedBy) {
        validateEmpresa(empresa);

        String nif = empresa.getNif().trim();
        empresa.setNif(nif);

        empresaRepository.findByNif(nif).ifPresent(existing -> {
            if (empresa.getId() == null || !existing.getId().equals(empresa.getId())) {
                throw new IllegalArgumentException("Já existe uma empresa registada com o NIF " + nif + ".");
            }
        });

        if (empresa.getMoedaBase() == null || empresa.getMoedaBase().isBlank()) {
            empresa.setMoedaBase("AOA");
        }
        if (empresa.getCasasDecimaisValor() == null) {
            empresa.setCasasDecimaisValor(2);
        }
        if (empresa.getCasasDecimaisQuantidade() == null) {
            empresa.setCasasDecimaisQuantidade(3);
        }
        if (empresa.getAtiva() && empresa.getExercicioActual() == null) {
            empresa.setExercicioActual(LocalDate.now().getYear());
        }

        Empresa saved = empresaRepository.save(empresa);

        if (abrirExercicio && saved.getExercicioActual() != null) {
            ensureFiscalYear(saved, saved.getExercicioActual(), updatedBy);
        }

        Map<String, String> allParameters = new LinkedHashMap<>();
        if (parameters != null) {
            allParameters.putAll(parameters);
        }

        allParameters.put("SETUP_CONCLUIDO", "true");
        allParameters.put("SETUP_VERSAO", "1");
        allParameters.put("BACKUP_EMPRESA_ENABLED", String.valueOf(backupEnabled));
        allParameters.put("BACKUP_EMPRESA_FREQUENCY", backupFrequency == null ? "DAILY" : backupFrequency);
        allParameters.put("BACKUP_EMPRESA_RETENTION_DAYS", String.valueOf(Math.max(1, backupRetentionDays)));
        allParameters.put("MFA_ADMIN_REQUIRED", String.valueOf(mfaAdminRequired));
        allParameters.put("MOEDA_BASE", nullSafe(saved.getMoedaBase()));
        allParameters.put("MOEDA_ALTERNATIVA", nullSafe(saved.getMoedaAlternativa()));
        allParameters.put("CASAS_DECIMAIS_VALOR", String.valueOf(
                saved.getCasasDecimaisValor() == null ? 2 : saved.getCasasDecimaisValor()));
        allParameters.put("CASAS_DECIMAIS_QUANTIDADE", String.valueOf(
                saved.getCasasDecimaisQuantidade() == null ? 3 : saved.getCasasDecimaisQuantidade()));

        for (Map.Entry<String, String> entry : allParameters.entrySet()) {
            upsertParameter(saved, entry.getKey(), entry.getValue(), parameterType(entry.getKey()),
                    parameterDescription(entry.getKey()), parameterGroup(entry.getKey()), updatedBy);
        }

        return saved;
    }

    private void ensureFiscalYear(Empresa empresa, Integer year, String updatedBy) {
        if (exercicioFiscalRepository.findByEmpresaIdAndAno(empresa.getId(), year).isPresent()) {
            return;
        }

        ExercicioFiscal exercicio = ExercicioFiscal.builder()
                .empresa(empresa)
                .ano(year)
                .dataInicio(LocalDate.of(year, 1, 1))
                .dataFim(LocalDate.of(year, 12, 31))
                .estado(ExercicioFiscal.EstadoExercicio.ABERTO)
                .observacoes("Criado pelo Assistente de Instalação de Empresa do Kubata.")
                .build();

        exercicioFiscalRepository.save(exercicio);
    }

    private void upsertParameter(Empresa empresa,
                                 String key,
                                 String value,
                                 String type,
                                 String description,
                                 String group,
                                 String updatedBy) {
        ParametroSistema parametro = parametroSistemaRepository
                .findByEmpresa_IdAndChave(empresa.getId(), key)
                .orElseGet(ParametroSistema::new);

        parametro.setEmpresa(empresa);
        parametro.setChave(key);
        parametro.setValor(value);
        parametro.setTipoValor(type);
        parametro.setDescricao(description);
        parametro.setGrupo(group);
        parametro.setEditavel(true);
        parametro.setAtualizadoEm(LocalDateTime.now());
        parametro.setAtualizadoPor(updatedBy == null || updatedBy.isBlank() ? "Administrador" : updatedBy);

        parametroSistemaRepository.save(parametro);
    }

    private void validateEmpresa(Empresa empresa) {
        if (empresa == null) {
            throw new IllegalArgumentException("A empresa não pode ser nula.");
        }
        if (empresa.getNome() == null || empresa.getNome().isBlank()) {
            throw new IllegalArgumentException("O nome/razão social da empresa é obrigatório.");
        }
        if (empresa.getNif() == null || empresa.getNif().isBlank()) {
            throw new IllegalArgumentException("O NIF da empresa é obrigatório.");
        }
        if (empresa.getIdentificador() == null || empresa.getIdentificador().isBlank()) {
            throw new IllegalArgumentException("O identificador da empresa é obrigatório.");
        }
    }

    private String parameterType(String key) {
        if (key.contains("ENABLED") || key.equals("SETUP_CONCLUIDO")
                || key.equals("MFA_ADMIN_REQUIRED")) {
            return "BOOLEAN";
        }
        if (key.contains("RETENTION_DAYS") || key.startsWith("CASAS_DECIMAIS_")) {
            return "INTEGER";
        }
        return "STRING";
    }

    private String parameterDescription(String key) {
        return switch (key) {
            case "SETUP_CONCLUIDO" -> "Indica que o assistente de instalação da empresa foi concluído.";
            case "SETUP_VERSAO" -> "Versão do processo de instalação da empresa.";
            case "BACKUP_EMPRESA_ENABLED" -> "Ativa o plano de backup da empresa.";
            case "BACKUP_EMPRESA_FREQUENCY" -> "Periodicidade definida para o backup da empresa.";
            case "BACKUP_EMPRESA_RETENTION_DAYS" -> "Número de dias de retenção dos backups da empresa.";
            case "MFA_ADMIN_REQUIRED" -> "Exige autenticação multifator para administradores.";
            case "MOEDA_BASE" -> "Moeda base utilizada pela empresa.";
            case "MOEDA_ALTERNATIVA" -> "Moeda alternativa configurada para a empresa.";
            case "CASAS_DECIMAIS_VALOR" -> "Casas decimais padrão para valores monetários.";
            case "CASAS_DECIMAIS_QUANTIDADE" -> "Casas decimais padrão para quantidades.";
            default -> "Parâmetro configurado pelo Assistente de Instalação de Empresa.";
        };
    }

    private String parameterGroup(String key) {
        if (key.startsWith("BACKUP") || key.startsWith("MFA")) return "SEGURANCA";
        if (key.startsWith("MOEDA") || key.startsWith("CASAS")) return "FINANCEIRO";
        if (key.startsWith("SETUP")) return "SISTEMA";
        return "SISTEMA";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
