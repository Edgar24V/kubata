package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.ExercicioFiscal;
import ao.allon.kubata.core.domain.ParametroSistema;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.util.NifUtils;
import ao.allon.kubata.core.util.LogoUtils;
import ao.allon.kubata.core.repository.ExercicioFiscalRepository;
import ao.allon.kubata.core.repository.ParametroSistemaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Map;
import java.util.Optional;

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

        String nif = NifUtils.normalize(empresa.getNif());
        empresa.setNif(nif);
        empresa.setNifFiscal(normalizeOptionalNif(empresa.getNifFiscal()));
        empresa.setNifSegurancaSocial(normalizeOptionalNif(empresa.getNifSegurancaSocial()));

        Optional<Empresa> duplicate = findDuplicateByNormalizedNif(empresa, nif);
        if (duplicate.isPresent()) {
            throw new IllegalArgumentException("Já existe uma empresa registada com o NIF " + nif + ".");
        }

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

        applyDefaultPolicyBeforeSave(empresa);
        Empresa saved = empresaRepository.save(empresa);
        saved = ensureDefaultCompany(saved);

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
        allParameters.put("TIPO_INSTALACAO", nullSafe(saved.getTipoInstalacao()));
        allParameters.put("DEMO_MODE", String.valueOf("DEMONSTRACAO".equals(saved.getTipoInstalacao())));
        if ("DEMONSTRACAO".equals(saved.getTipoInstalacao())) {
            // Registo explícito: não injectar amostras nas tabelas operacionais globais,
            // pois os módulos ainda não isolam todas as entidades por empresa.
            allParameters.put("DEMO_DADOS_OPERACIONAIS", "NAO_GERADOS");
        } else {
            allParameters.remove("DEMO_DADOS_OPERACIONAIS");
        }
        allParameters.put("MOEDA_BASE", nullSafe(saved.getMoedaBase()));
        allParameters.put("MOEDA_ALTERNATIVA", nullSafe(saved.getMoedaAlternativa()));
        allParameters.put("CAMBIO_TAXA", saved.getTaxaCambio() == null ? "" : saved.getTaxaCambio().toPlainString());
        allParameters.put("CAMBIO_SENTIDO", nullSafe(saved.getSentidoCambio()));
        allParameters.put("IVA_PERIODICIDADE", nullSafe(saved.getPeriodicidadeIva()));
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

    /**
     * Altera de forma centralizada o estado operacional de uma empresa.
     *
     * <p>O Administrator não permite que a última empresa activa seja
     * desactivada, evitando que módulos dependentes de uma empresa operacional
     * fiquem sem contexto empresarial.</p>
     */
    @Transactional
    public Empresa alterarEstado(Long empresaId, boolean activa) {
        if (empresaId == null) {
            throw new IllegalArgumentException("A empresa seleccionada não possui identificador.");
        }

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));

        if (empresa.getAtiva() == activa) {
            return empresa;
        }

        if (!activa && empresaRepository.countByAtivaTrue() <= 1) {
            throw new IllegalStateException(
                    "Não é possível desactivar a última empresa activa. "
                            + "Active outra empresa antes de desactivar esta."
            );
        }

        empresa.setAtiva(activa);
        return empresaRepository.save(empresa);
    }

    private void applyDefaultPolicyBeforeSave(Empresa empresa) {
        if (!empresa.getAtiva()) {
            empresa.setPredefinida(false);
            return;
        }

        if (empresa.getPredefinida()) {
            clearOtherDefaultCompanies(empresa.getId());
            return;
        }

        if (empresa.getId() == null && empresaRepository.findFirstByPredefinidaTrueAndAtivaTrue().isEmpty()) {
            empresa.setPredefinida(true);
        }
    }

    private Empresa ensureDefaultCompany(Empresa saved) {
        Optional<Empresa> currentDefault = empresaRepository.findFirstByPredefinidaTrueAndAtivaTrue();
        if (currentDefault.isPresent()) {
            return saved;
        }

        // Corrige eventuais marcas antigas de predefinição em empresas inactivas.
        clearOtherDefaultCompanies(null);

        if (saved.getAtiva()) {
            saved.setPredefinida(true);
            return empresaRepository.save(saved);
        }

        Optional<Empresa> fallback = empresaRepository.findAllByAtivaTrueOrderByNomeAsc()
                .stream()
                .findFirst();

        if (fallback.isPresent()) {
            Empresa defaultCompany = fallback.get();
            defaultCompany.setPredefinida(true);
            empresaRepository.save(defaultCompany);
        }

        return saved;
    }

    private void clearOtherDefaultCompanies(Long currentId) {
        for (Empresa other : empresaRepository.findAllByPredefinidaTrue()) {
            if (currentId == null || !currentId.equals(other.getId())) {
                other.setPredefinida(false);
                empresaRepository.save(other);
            }
        }
    }

    /**
     * Torna explicitamente uma empresa activa na empresa predefinida do
     * ambiente. Existe apenas uma empresa predefinida.
     */
    @Transactional
    public Empresa definirComoPredefinida(Long empresaId) {
        if (empresaId == null) {
            throw new IllegalArgumentException("A empresa seleccionada não possui identificador.");
        }

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));

        if (!empresa.getAtiva()) {
            throw new IllegalStateException("Só uma empresa activa pode ser definida como predefinida.");
        }

        clearOtherDefaultCompanies(empresaId);
        empresa.setPredefinida(true);
        return empresaRepository.save(empresa);
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

        if (isBlank(empresa.getTipoInstalacao())) empresa.setTipoInstalacao("STANDARD");
        else empresa.setTipoInstalacao(empresa.getTipoInstalacao().trim().toUpperCase(java.util.Locale.ROOT));
        if (!Set.of("STANDARD", "DEMONSTRACAO").contains(empresa.getTipoInstalacao())) {
            throw new IllegalArgumentException("Seleccione uma instalação Standard ou Demonstração.");
        }

        if (isBlank(empresa.getPais())) empresa.setPais("AO");
        empresa.setPais(empresa.getPais().trim().toUpperCase(java.util.Locale.ROOT));
        if (!Set.of("AO", "PT", "MZ", "NA", "ZA", "CV", "BR", "US", "GB").contains(empresa.getPais())) {
            throw new IllegalArgumentException("O país seleccionado não é suportado pelo assistente.");
        }

        if (isBlank(empresa.getPeriodicidadeIva())) empresa.setPeriodicidadeIva("MENSAL");
        empresa.setPeriodicidadeIva(empresa.getPeriodicidadeIva().trim().toUpperCase(java.util.Locale.ROOT));
        if (!Set.of("MENSAL", "TRIMESTRAL", "ISENTO").contains(empresa.getPeriodicidadeIva())) {
            throw new IllegalArgumentException("A periodicidade do IVA deve ser mensal, trimestral ou isenta/não aplicável.");
        }

        if (isBlank(empresa.getSentidoCambio())) empresa.setSentidoCambio("INVERTIDO");
        empresa.setSentidoCambio(empresa.getSentidoCambio().trim().toUpperCase(java.util.Locale.ROOT));
        if (!Set.of("DIRECTO", "INVERTIDO").contains(empresa.getSentidoCambio())) {
            throw new IllegalArgumentException("O sentido do câmbio deve ser Directo ou Invertido.");
        }

        if (isBlank(empresa.getNome())) {
            throw new IllegalArgumentException("O nome/razão social da empresa é obrigatório.");
        }
        if (empresa.getNif() == null || empresa.getNif().isBlank()) {
            throw new IllegalArgumentException("O NIF da empresa é obrigatório.");
        }
        NifUtils.TipoContribuinte nifType = nifTypeOf(empresa.getTipoContribuinte());
        String nifMessage = validateTaxIdForCountry(empresa.getNif(), nifType, empresa.getPais());
        if (nifMessage != null) {
            throw new IllegalArgumentException(nifMessage);
        }
        if (isBlank(empresa.getIdentificador()) || !empresa.getIdentificador().trim().matches("(?i)[a-z]{3}")) {
            throw new IllegalArgumentException("O identificador da empresa deve conter exactamente três letras (A–Z).");
        }
        empresa.setIdentificador(empresa.getIdentificador().trim().toUpperCase(java.util.Locale.ROOT));
        boolean duplicateIdentifier = empresaRepository.findAll().stream()
                .filter(existing -> existing.getIdentificador() != null)
                .filter(existing -> empresa.getId() == null || !empresa.getId().equals(existing.getId()))
                .anyMatch(existing -> empresa.getIdentificador().equalsIgnoreCase(existing.getIdentificador()));
        if (duplicateIdentifier) {
            throw new IllegalArgumentException("O identificador " + empresa.getIdentificador() + " já está atribuído a outra empresa.");
        }

        if (isBlank(empresa.getMoedaBase())) {
            empresa.setMoedaBase("AOA");
        }
        if (!empresa.getMoedaBase().trim().matches("(?i)[A-Z]{3}")) {
            throw new IllegalArgumentException("A moeda base deve ser um código ISO de três letras.");
        }
        empresa.setMoedaBase(empresa.getMoedaBase().trim().toUpperCase(java.util.Locale.ROOT));
        if (!isBlank(empresa.getMoedaAlternativa())) {
            empresa.setMoedaAlternativa(empresa.getMoedaAlternativa().trim().toUpperCase(java.util.Locale.ROOT));
            if (!empresa.getMoedaAlternativa().matches("[A-Z]{3}")) {
                throw new IllegalArgumentException("A moeda alternativa deve ser um código ISO de três letras.");
            }
            if (empresa.getMoedaBase().equals(empresa.getMoedaAlternativa())) {
                throw new IllegalArgumentException("A moeda alternativa deve ser diferente da moeda base.");
            }
            if (empresa.getTaxaCambio() == null || empresa.getTaxaCambio().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Defina uma taxa de câmbio superior a zero para a moeda alternativa.");
            }
        } else {
            empresa.setMoedaAlternativa(null);
            empresa.setTaxaCambio(null);
        }
        if (empresa.getTaxaCambio() != null && empresa.getTaxaCambio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("A taxa de câmbio deve ser superior a zero.");
        }
        if (empresa.getPredefinida() && !empresa.getAtiva()) {
            throw new IllegalArgumentException("Uma empresa inactiva não pode ser definida como empresa predefinida.");
        }
        if (empresa.getRodapeDocumento() != null
                && empresa.getRodapeDocumento().length() > Empresa.LIMITE_RODAPE_DOCUMENTO) {
            throw new IllegalArgumentException(
                    "O rodapé dos documentos não pode exceder " + Empresa.LIMITE_RODAPE_DOCUMENTO + " caracteres."
            );
        }
        if (isBlank(empresa.getProvincia())) {
            throw new IllegalArgumentException("A província ou região da empresa é obrigatória.");
        }
        if ("AO".equals(empresa.getPais()) && !isAngolaProvince(empresa.getProvincia())) {
            throw new IllegalArgumentException("A província da empresa deve ser uma província válida de Angola.");
        }
        if (isBlank(empresa.getMunicipio())) {
            throw new IllegalArgumentException("O município ou cidade da empresa é obrigatório.");
        }
        if (isBlank(empresa.getRegimeFiscal())) {
            throw new IllegalArgumentException("O regime fiscal da empresa é obrigatório.");
        }
        if (!Set.of(
                "Regime Geral",
                "Regime Simplificado",
                "Regime de Exclusão",
                "Isento",
                "Especial"
        ).contains(empresa.getRegimeFiscal().trim())) {
            throw new IllegalArgumentException("O regime fiscal seleccionado não é suportado pelo Administrator.");
        }
        if (!isBlank(empresa.getCae()) && !empresa.getCae().trim().matches("\\d{2,10}")) {
            throw new IllegalArgumentException("CAE inválido: use apenas algarismos, entre 2 e 10 caracteres.");
        }
        if (!isBlank(empresa.getNifFiscal())) {
            String message = validateTaxIdForCountry(empresa.getNifFiscal(), nifType, empresa.getPais());
            if (message != null) {
                throw new IllegalArgumentException("NIF Fiscal inválido: " + message);
            }
        }
        if (!isBlank(empresa.getNifSegurancaSocial())) {
            String message = validateTaxIdForCountry(empresa.getNifSegurancaSocial(), nifType, empresa.getPais());
            if (message != null) {
                throw new IllegalArgumentException("NIF da Segurança Social inválido: " + message);
            }
        }
        String logoMessage = LogoUtils.validate(empresa.getLogotipo());
        if (logoMessage != null) {
            throw new IllegalArgumentException("Logótipo inválido: " + logoMessage);
        }

        if (empresa.getLogotipo() != null && empresa.getLogotipo().length > 0) {
            empresa.setLogotipoMimeType(LogoUtils.detectMimeType(empresa.getLogotipo()));
        } else {
            empresa.setLogotipoMimeType(null);
        }

        validateAgtCertificate(empresa);
    }

    private void validateAgtCertificate(Empresa empresa) {
        boolean any = !isBlank(empresa.getNumeroCertificadoAGT())
                || !isBlank(empresa.getVersaoCertificadoAGT())
                || empresa.getDataCertificadoAGT() != null
                || !isBlank(empresa.getHashCertificadoAGT());

        if (!any) {
            return;
        }

        if (isBlank(empresa.getNumeroCertificadoAGT())
                || isBlank(empresa.getVersaoCertificadoAGT())
                || empresa.getDataCertificadoAGT() == null
                || isBlank(empresa.getHashCertificadoAGT())) {
            throw new IllegalArgumentException("Os dados do certificado AGT devem ser preenchidos integralmente.");
        }

        if (empresa.getDataCertificadoAGT().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("A data do certificado AGT não pode estar no futuro.");
        }

        if (!empresa.getHashCertificadoAGT().trim().matches("(?i)[0-9a-f]{64}")) {
            throw new IllegalArgumentException("O hash do certificado AGT deve conter 64 caracteres hexadecimais.");
        }
    }

    private String validateTaxIdForCountry(String value,
                                           NifUtils.TipoContribuinte taxpayerType,
                                           String countryCode) {
        if ("AO".equalsIgnoreCase(countryCode)) {
            return NifUtils.validate(value, taxpayerType);
        }
        String normalized = NifUtils.normalize(value);
        return normalized.matches("[A-Z0-9]{6,30}")
                ? null
                : "O identificador fiscal deve conter entre 6 e 30 letras ou algarismos para o país seleccionado.";
    }

    private NifUtils.TipoContribuinte nifTypeOf(String value) {
        if ("Pessoa Singular".equalsIgnoreCase(value)) {
            return NifUtils.TipoContribuinte.PESSOA_SINGULAR;
        }
        if ("Não Residente".equalsIgnoreCase(value)) {
            return NifUtils.TipoContribuinte.NAO_RESIDENTE;
        }
        return NifUtils.TipoContribuinte.PESSOA_COLECTIVA;
    }

    private Optional<Empresa> findDuplicateByNormalizedNif(Empresa current, String normalizedNif) {
        return empresaRepository.findAll().stream()
                .filter(existing -> existing.getNif() != null)
                .filter(existing -> current.getId() == null || !existing.getId().equals(current.getId()))
                .filter(existing -> normalizedNif.equals(NifUtils.normalize(existing.getNif())))
                .findFirst();
    }

    private String normalizeOptionalNif(String value) {
        if (isBlank(value)) {
            return null;
        }
        return NifUtils.normalize(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isAngolaProvince(String value) {
        String province = value == null ? "" : value.trim();
        return Set.of(
                "Bengo", "Benguela", "Bié", "Cabinda", "Cuando", "Cubango",
                "Cuanza Norte", "Cuanza Sul", "Cunene", "Huambo", "Huíla",
                "Icolo e Bengo", "Luanda", "Lunda Norte", "Lunda Sul", "Malanje",
                "Moxico", "Moxico Leste", "Namibe", "Uíge", "Zaire",
                "Cuando Cubango"
        ).stream().anyMatch(candidate -> candidate.equalsIgnoreCase(province));
    }

    private String parameterType(String key) {
        if (key.contains("ENABLED") || key.equals("SETUP_CONCLUIDO")
                || key.equals("MFA_ADMIN_REQUIRED") || key.equals("DEMO_MODE")) {
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
            case "CAMBIO_TAXA" -> "Taxa de câmbio da moeda alternativa.";
            case "CAMBIO_SENTIDO" -> "Convenção directa ou invertida da taxa de câmbio.";
            case "IVA_PERIODICIDADE" -> "Periodicidade de entrega ou enquadramento do IVA.";
            case "TIPO_INSTALACAO" -> "Tipo de instalação: Standard ou Demonstração.";
            case "DEMO_MODE" -> "Indica se a empresa foi criada para demonstração.";
            case "DEMO_DADOS_OPERACIONAIS" -> "Estado da carga de dados operacionais demonstrativos.";
            case "CASAS_DECIMAIS_VALOR" -> "Casas decimais padrão para valores monetários.";
            case "CASAS_DECIMAIS_QUANTIDADE" -> "Casas decimais padrão para quantidades.";
            default -> "Parâmetro configurado pelo Assistente de Instalação de Empresa.";
        };
    }

    private String parameterGroup(String key) {
        if (key.startsWith("BACKUP") || key.startsWith("MFA")) return "SEGURANCA";
        if (key.startsWith("MOEDA") || key.startsWith("CASAS") || key.startsWith("CAMBIO")) return "FINANCEIRO";
        if (key.startsWith("IVA_")) return "FISCAL";
        if (key.startsWith("DEMO_") || key.equals("TIPO_INSTALACAO") || key.startsWith("SETUP")) return "SISTEMA";
        return "SISTEMA";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
