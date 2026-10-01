package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.Alerta;
import ao.allon.kubata.core.domain.AlertaHistorico;
import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.PermissaoPerfil;
import ao.allon.kubata.core.repository.AlertaHistoricoRepository;
import ao.allon.kubata.core.repository.AlertaRepository;
import ao.allon.kubata.core.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class AlertaService {

    public static final String MODULE = "ADMINISTRATOR";
    public static final String RESOURCE = "ALERTAS";

    private final AlertaRepository alertaRepository;
    private final AlertaHistoricoRepository historicoRepository;
    private final UserRepository userRepository;
    private final AcessoService acessoService;
    private final AuditService auditService;

    public AlertaService(
            AlertaRepository alertaRepository,
            AlertaHistoricoRepository historicoRepository,
            UserRepository userRepository,
            AcessoService acessoService,
            AuditService auditService) {
        this.alertaRepository = alertaRepository;
        this.historicoRepository = historicoRepository;
        this.userRepository = userRepository;
        this.acessoService = acessoService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Alerta> listar(User actor) {
        require(actor, PermissaoPerfil.Operacao.VER);
        return alertaRepository.findAllByOrderByOpenedAtDescIdDesc();
    }

    @Transactional(readOnly = true)
    public List<Alerta> listarPorEstado(User actor, Alerta.Estado estado) {
        require(actor, PermissaoPerfil.Operacao.VER);
        if (estado == null) {
            return listarInternal();
        }
        return alertaRepository.findByEstadoOrderByOpenedAtDescIdDesc(estado);
    }

    @Transactional(readOnly = true)
    public List<User> responsaveis(User actor) {
        require(actor, PermissaoPerfil.Operacao.VER);
        return userRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .sorted((a, b) -> safe(a.getNome()).compareToIgnoreCase(safe(b.getNome())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Alerta.Estado, Long> resumo(User actor) {
        require(actor, PermissaoPerfil.Operacao.VER);
        Map<Alerta.Estado, Long> result = new EnumMap<>(Alerta.Estado.class);
        for (Alerta.Estado estado : Alerta.Estado.values()) {
            result.put(estado, alertaRepository.countByEstado(estado));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<AlertaHistorico> historico(User actor, Long alertaId) {
        require(actor, PermissaoPerfil.Operacao.VER);
        requireId(alertaId);
        if (!alertaRepository.existsById(alertaId)) {
            throw new IllegalArgumentException("Alerta não encontrado.");
        }
        return historicoRepository.findByAlertaIdOrderByEventAtAscIdAsc(alertaId);
    }

    @Transactional
    public Alerta criar(
            User actor,
            String codigo,
            String titulo,
            String descricao,
            Alerta.Severidade severidade,
            String origem,
            String referencia,
            User responsavel) {

        require(actor, PermissaoPerfil.Operacao.CRIAR);
        validar(codigo, titulo, descricao, origem);
        validarResponsavel(responsavel);

        String normalizedCode = codigo.trim();
        if (alertaRepository.existsByCodigoIgnoreCase(normalizedCode)) {
            throw new IllegalArgumentException("Já existe um alerta com o código '" + normalizedCode + "'.");
        }

        Alerta alerta = new Alerta();
        alerta.setCodigo(normalizedCode);
        alerta.setTitulo(titulo.trim());
        alerta.setDescricao(blankToNull(descricao));
        alerta.setSeveridade(severidade == null ? Alerta.Severidade.MEDIUM : severidade);
        alerta.setEstado(Alerta.Estado.OPEN);
        alerta.setOrigem(origem.trim());
        alerta.setReferencia(blankToNull(referencia));
        alerta.setResponsavel(responsavel);
        alerta.setOpenedAt(LocalDateTime.now());

        Alerta saved = alertaRepository.save(alerta);
        registarHistorico(saved, AlertaHistorico.Acao.CREATED, null, saved.getEstado(), actor,
                "Alerta criado.");
        auditar(actor, AuditLog.AuditActionType.CREATE, saved,
                null, payload(saved), "Criação de alerta");
        return saved;
    }

    /**
     * Cria um alerta produzido pelo runtime/sistema, sem depender de uma sessão humana.
     * A autorização aplica-se às operações administrativas; eventos de sistema são internos.
     */
    @Transactional
    public Alerta criarSistema(
            String codigo,
            String titulo,
            String descricao,
            Alerta.Severidade severidade,
            String origem,
            String referencia) {

        validar(codigo, titulo, descricao, origem);
        if (alertaRepository.existsByCodigoIgnoreCase(codigo.trim())) {
            return alertaRepository.findByCodigoIgnoreCase(codigo.trim()).orElseThrow();
        }

        Alerta alerta = new Alerta();
        alerta.setCodigo(codigo.trim());
        alerta.setTitulo(titulo.trim());
        alerta.setDescricao(blankToNull(descricao));
        alerta.setSeveridade(severidade == null ? Alerta.Severidade.HIGH : severidade);
        alerta.setEstado(Alerta.Estado.OPEN);
        alerta.setOrigem(origem.trim());
        alerta.setReferencia(blankToNull(referencia));
        alerta.setOpenedAt(LocalDateTime.now());

        Alerta saved = alertaRepository.save(alerta);
        registarHistorico(saved, AlertaHistorico.Acao.CREATED, null, saved.getEstado(), null,
                "Alerta gerado automaticamente pelo sistema.");
        auditar(null, AuditLog.AuditActionType.CREATE, saved,
                null, payload(saved), "Criação automática de alerta");
        return saved;
    }

    @Transactional
    public Alerta reconhecer(User actor, Long alertaId, String observacao) {
        require(actor, PermissaoPerfil.Operacao.EDITAR);
        Alerta alerta = carregar(alertaId);
        if (alerta.getEstado() != Alerta.Estado.OPEN) {
            throw new IllegalStateException("Só alertas OPEN podem ser reconhecidos.");
        }

        Alerta.Estado anterior = alerta.getEstado();
        alerta.setEstado(Alerta.Estado.ACKNOWLEDGED);
        alerta.setReconhecidoPor(actor);
        alerta.setAcknowledgedAt(LocalDateTime.now());
        alerta.setUltimaObservacao(blankToNull(observacao));

        Alerta saved = alertaRepository.save(alerta);
        registarHistorico(saved, AlertaHistorico.Acao.ACKNOWLEDGED, anterior, saved.getEstado(), actor,
                blankToDefault(observacao, "Alerta reconhecido."));
        auditar(actor, AuditLog.AuditActionType.UPDATE, saved,
                estadoPayload(anterior), estadoPayload(saved.getEstado()), "Reconhecimento de alerta");
        return saved;
    }

    @Transactional
    public Alerta resolver(User actor, Long alertaId, String observacao) {
        require(actor, PermissaoPerfil.Operacao.EDITAR);
        Alerta alerta = carregar(alertaId);
        if (alerta.getEstado() != Alerta.Estado.OPEN
                && alerta.getEstado() != Alerta.Estado.ACKNOWLEDGED) {
            throw new IllegalStateException("Só alertas OPEN ou ACKNOWLEDGED podem ser resolvidos.");
        }

        Alerta.Estado anterior = alerta.getEstado();
        alerta.setEstado(Alerta.Estado.RESOLVED);
        alerta.setResolvidoPor(actor);
        alerta.setResolvedAt(LocalDateTime.now());
        alerta.setUltimaObservacao(blankToDefault(observacao, "Alerta resolvido."));

        Alerta saved = alertaRepository.save(alerta);
        registarHistorico(saved, AlertaHistorico.Acao.RESOLVED, anterior, saved.getEstado(), actor,
                saved.getUltimaObservacao());
        auditar(actor, AuditLog.AuditActionType.UPDATE, saved,
                estadoPayload(anterior), estadoPayload(saved.getEstado()), "Resolução de alerta");
        return saved;
    }

    @Transactional
    public Alerta ignorar(User actor, Long alertaId, String observacao) {
        require(actor, PermissaoPerfil.Operacao.EDITAR);
        Alerta alerta = carregar(alertaId);
        if (alerta.getEstado() != Alerta.Estado.OPEN
                && alerta.getEstado() != Alerta.Estado.ACKNOWLEDGED) {
            throw new IllegalStateException("Só alertas OPEN ou ACKNOWLEDGED podem ser ignorados.");
        }

        Alerta.Estado anterior = alerta.getEstado();
        alerta.setEstado(Alerta.Estado.IGNORED);
        alerta.setIgnoradoPor(actor);
        alerta.setIgnoredAt(LocalDateTime.now());
        alerta.setUltimaObservacao(blankToDefault(observacao, "Alerta ignorado."));

        Alerta saved = alertaRepository.save(alerta);
        registarHistorico(saved, AlertaHistorico.Acao.IGNORED, anterior, saved.getEstado(), actor,
                saved.getUltimaObservacao());
        auditar(actor, AuditLog.AuditActionType.UPDATE, saved,
                estadoPayload(anterior), estadoPayload(saved.getEstado()), "Ignorar alerta");
        return saved;
    }

    @Transactional
    public Alerta reabrir(User actor, Long alertaId, String observacao) {
        require(actor, PermissaoPerfil.Operacao.EDITAR);
        Alerta alerta = carregar(alertaId);
        if (alerta.getEstado() != Alerta.Estado.RESOLVED
                && alerta.getEstado() != Alerta.Estado.IGNORED) {
            throw new IllegalStateException("Só alertas RESOLVED ou IGNORED podem ser reabertos.");
        }

        Alerta.Estado anterior = alerta.getEstado();
        alerta.setEstado(Alerta.Estado.OPEN);
        alerta.setReopenedAt(LocalDateTime.now());
        alerta.setReconhecidoPor(null);
        alerta.setAcknowledgedAt(null);
        alerta.setResolvidoPor(null);
        alerta.setResolvedAt(null);
        alerta.setIgnoradoPor(null);
        alerta.setIgnoredAt(null);
        alerta.setUltimaObservacao(blankToDefault(observacao, "Alerta reaberto."));

        Alerta saved = alertaRepository.save(alerta);
        registarHistorico(saved, AlertaHistorico.Acao.REOPENED, anterior, saved.getEstado(), actor,
                saved.getUltimaObservacao());
        auditar(actor, AuditLog.AuditActionType.UPDATE, saved,
                estadoPayload(anterior), estadoPayload(saved.getEstado()), "Reabertura de alerta");
        return saved;
    }

    @Transactional
    public Alerta atribuirResponsavel(User actor, Long alertaId, User responsavel, String observacao) {
        require(actor, PermissaoPerfil.Operacao.EDITAR);
        validarResponsavel(responsavel);
        Alerta alerta = carregar(alertaId);

        Long anteriorId = alerta.getResponsavel() == null ? null : alerta.getResponsavel().getId();
        alerta.setResponsavel(responsavel);
        alerta.setUltimaObservacao(blankToDefault(observacao,
                responsavel == null ? "Responsável removido." : "Responsável atribuído."));

        Alerta saved = alertaRepository.save(alerta);
        registarHistorico(saved, AlertaHistorico.Acao.ASSIGNED, saved.getEstado(), saved.getEstado(), actor,
                saved.getUltimaObservacao());

        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("responsavelId", anteriorId);
        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("responsavelId", responsavel == null ? null : responsavel.getId());
        newValues.put("responsavelNome", responsavel == null ? null : responsavel.getNome());
        auditar(actor, AuditLog.AuditActionType.UPDATE, saved, oldValues, newValues,
                "Alteração do responsável do alerta");
        return saved;
    }

    private List<Alerta> listarInternal() {
        return alertaRepository.findAllByOrderByOpenedAtDescIdDesc();
    }

    private Alerta carregar(Long id) {
        requireId(id);
        return alertaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alerta não encontrado."));
    }

    private void require(User actor, PermissaoPerfil.Operacao operation) {
        if (actor == null) {
            throw new SecurityException("Sessão administrativa não autenticada.");
        }
        if (actor.isSuperadmin() || actor.getRole() == ao.allon.kubata.core.domain.Role.ADMIN) {
            return;
        }
        if (!acessoService.temAcesso(actor, MODULE, RESOURCE, operation)) {
            throw new SecurityException("Permissão insuficiente para gerir alertas.");
        }
    }

    private void validar(String codigo, String titulo, String descricao, String origem) {
        if (isBlank(codigo)) {
            throw new IllegalArgumentException("O código do alerta é obrigatório.");
        }
        if (codigo.trim().length() > 120) {
            throw new IllegalArgumentException("O código do alerta não pode exceder 120 caracteres.");
        }
        if (isBlank(titulo)) {
            throw new IllegalArgumentException("O título do alerta é obrigatório.");
        }
        if (titulo.trim().length() > 255) {
            throw new IllegalArgumentException("O título do alerta não pode exceder 255 caracteres.");
        }
        if (isBlank(descricao)) {
            throw new IllegalArgumentException("A descrição do alerta é obrigatória.");
        }
        if (descricao.length() > 10000) {
            throw new IllegalArgumentException("A descrição do alerta é demasiado longa.");
        }
        if (isBlank(origem)) {
            throw new IllegalArgumentException("A origem do alerta é obrigatória.");
        }
        if (origem.trim().length() > 120) {
            throw new IllegalArgumentException("A origem do alerta não pode exceder 120 caracteres.");
        }
    }

    private void validarResponsavel(User responsavel) {
        if (responsavel != null && !Boolean.TRUE.equals(responsavel.getActive())) {
            throw new IllegalArgumentException("O responsável seleccionado está inactivo.");
        }
    }

    private void requireId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Identificador de alerta inválido.");
        }
    }

    private void registarHistorico(
            Alerta alerta,
            AlertaHistorico.Acao acao,
            Alerta.Estado anterior,
            Alerta.Estado novo,
            User actor,
            String observacao) {

        AlertaHistorico historico = new AlertaHistorico();
        historico.setAlerta(alerta);
        historico.setAcao(acao);
        historico.setEstadoAnterior(anterior);
        historico.setEstadoNovo(novo);
        historico.setUtilizador(actor);
        historico.setObservacao(blankToNull(observacao));
        historico.setEventAt(LocalDateTime.now());
        historicoRepository.save(historico);
    }

    private void auditar(
            User actor,
            AuditLog.AuditActionType type,
            Alerta alerta,
            Object oldValues,
            Object newValues,
            String description) {

        auditService.logAction(
                actor,
                actor == null ? "SYSTEM" : actor.getNome(),
                type,
                "ALERTA",
                alerta.getId() == null ? alerta.getCodigo() : String.valueOf(alerta.getId()),
                description + ": " + alerta.getCodigo(),
                oldValues,
                newValues,
                MODULE,
                null,
                "Kubata Administrator",
                null,
                false,
                AuditLog.AGTComplianceLevel.HIGH
        );
    }

    private Map<String, Object> payload(Alerta alerta) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("codigo", alerta.getCodigo());
        payload.put("titulo", alerta.getTitulo());
        payload.put("severidade", alerta.getSeveridade() == null ? null : alerta.getSeveridade().name());
        payload.put("estado", alerta.getEstado() == null ? null : alerta.getEstado().name());
        payload.put("origem", alerta.getOrigem());
        payload.put("responsavelId", alerta.getResponsavel() == null ? null : alerta.getResponsavel().getId());
        payload.put("resolvidoPorId", alerta.getResolvidoPor() == null ? null : alerta.getResolvidoPor().getId());
        return payload;
    }

    private Map<String, Object> estadoPayload(Alerta.Estado estado) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("estado", estado == null ? null : estado.name());
        return payload;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String blankToDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }
}
