package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.Filial;
import ao.allon.kubata.core.domain.PerfilAcesso;
import ao.allon.kubata.core.domain.PermissaoPerfil;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.TipoConta;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserSession;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.FilialRepository;
import ao.allon.kubata.core.repository.PerfilAcessoRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.repository.UserSessionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserAdministrationService {

    public static final String MODULE = "ADMINISTRATOR";
    public static final String RESOURCE = "UTILIZADORES";

    private final UserRepository userRepository;
    private final EmpresaRepository empresaRepository;
    private final FilialRepository filialRepository;
    private final PerfilAcessoRepository perfilRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityService securityService;
    private final AuditService auditService;

    public UserAdministrationService(UserRepository userRepository,
                                     EmpresaRepository empresaRepository,
                                     FilialRepository filialRepository,
                                     PerfilAcessoRepository perfilRepository,
                                     UserSessionRepository userSessionRepository,
                                     PasswordEncoder passwordEncoder,
                                     SecurityService securityService,
                                     AuditService auditService) {
        this.userRepository = userRepository;
        this.empresaRepository = empresaRepository;
        this.filialRepository = filialRepository;
        this.perfilRepository = perfilRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityService = securityService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<User> listar() {
        return userRepository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public User carregarParaEdicao(Long id) {
        return userRepository.findByIdWithPerfis(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<Filial> filiais(Empresa empresa) {
        if (empresa == null) return List.of();
        return filialRepository.findByEmpresaOrderByNomeAsc(empresa);
    }

    @Transactional
    public User salvar(User actor,
                       User draft,
                       String rawPassword,
                       Set<PerfilAcesso> requestedProfiles,
                       Empresa requestedEmpresa,
                       Filial requestedFilial,
                       String sourceIp) {
        boolean isNew = draft == null || draft.getId() == null;
        require(actor, isNew ? "CRIAR" : "EDITAR");

        User managedActor = managedActor(actor);
        if (draft == null) {
            throw new IllegalArgumentException("Utilizador inválido.");
        }

        User target;
        if (isNew) {
            target = new User();
            target.setCodigo(generateCode());
        } else {
            target = userRepository.findByIdWithPerfis(draft.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));
        }

        if (!isNew && isSelf(managedActor, target) && !Boolean.TRUE.equals(draft.getActive())) {
            throw new IllegalArgumentException("A própria conta não pode ser desactivada.");
        }

        boolean targetWasSuperadmin = target.isSuperadmin();
        if ((draft.isSuperadmin() != targetWasSuperadmin || draft.getRole() != target.getRole())
                && !managedActor.isSuperadmin()) {
            throw new SecurityException(
                    "A alteração de privilégios de Superadministrador só pode ser feita por um Superadministrador."
            );
        }

        String nome = normalize(draft.getNome());
        String email = normalizeEmail(draft.getEmail());
        if (nome.length() < 2) throw new IllegalArgumentException("O nome deve ter pelo menos 2 caracteres.");
        if (!email.contains("@") || email.indexOf('@') <= 0) throw new IllegalArgumentException("Email inválido.");
        if (draft.getRole() == null) throw new IllegalArgumentException("A função da conta é obrigatória.");

        if (!isNew && target.getEmail() != null
                && email.equalsIgnoreCase(target.getEmail())) {
            // preserva a unicidade sem exigir nova consulta;
        } else if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Já existe um utilizador com este email.");
        }

        Empresa empresa = requestedEmpresa != null && requestedEmpresa.getId() != null
                ? empresaRepository.findById(requestedEmpresa.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."))
                : null;
        if (empresa == null || !empresa.getAtiva()) {
            throw new IllegalArgumentException("A empresa seleccionada não está activa.");
        }

        Filial filial = null;
        if (requestedFilial != null && requestedFilial.getId() != null) {
            filial = filialRepository.findById(requestedFilial.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Filial não encontrada."));
            if (!Boolean.TRUE.equals(filial.getActive()) || filial.getEmpresa() == null
                    || !filial.getEmpresa().getId().equals(empresa.getId())) {
                throw new IllegalArgumentException("A filial seleccionada não pertence à empresa activa.");
            }
        }

        TipoConta tipoConta = draft.getTipoConta() == null
                ? TipoConta.PESSOAL
                : draft.getTipoConta();

        byte[] avatar = draft.getAvatar();
        if (avatar != null && avatar.length > 2_000_000) {
            throw new IllegalArgumentException("A fotografia não pode ultrapassar 2 MB.");
        }

        Set<PerfilAcesso> profiles = validateProfiles(requestedProfiles, empresa);

        LinkedHashMap<String, Object> oldSnapshot = isNew ? null : userSnapshot(target);
        String oldPasswordHash = target.getPassword();
        if (rawPassword != null && !rawPassword.isBlank()) {
            validatePassword(rawPassword);
            target.setPassword(passwordEncoder.encode(rawPassword));
            target.setPasswordChangedAt(LocalDateTime.now());
        } else if (isNew) {
            throw new IllegalArgumentException("A palavra-passe inicial é obrigatória.");
        } else {
            target.setPassword(oldPasswordHash);
        }

        target.setCodigo(normalizeCode(draft.getCodigo(), target.getCodigo()));
        target.setNome(nome);
        target.setEmail(email);
        target.setEmpresa(empresa);
        target.setFilial(filial);
        target.setRole(draft.getRole());
        target.setTipoConta(tipoConta);
        target.setNif(blankToNull(draft.getNif()));
        target.setTelefone(blankToNull(draft.getTelefone()));
        target.setDepartamento(blankToNull(draft.getDepartamento()));
        target.setCargo(blankToNull(draft.getCargo()));
        target.setAvatar(avatar);
        target.setActive(Boolean.TRUE.equals(draft.getActive()));
        target.setIdioma(blankToNull(draft.getIdioma()) == null ? "pt-AO" : draft.getIdioma().trim());
        target.setTema(blankToNull(draft.getTema()) == null ? "VERDE_ADMIN" : draft.getTema().trim());
        target.setLinhasPorPagina(Math.max(10, Math.min(500, draft.getLinhasPorPagina())));
        target.setPasswordProvisoria(draft.isPasswordProvisoria());
        target.setDataExpiracaoPassword(draft.getDataExpiracaoPassword());
        target.setSuperadmin(draft.isSuperadmin());
        target.setPerfis(profiles);

        if (target.isPasswordProvisoria() && target.getDataExpiracaoPassword() == null) {
            target.setDataExpiracaoPassword(LocalDate.now().plusDays(90));
        }
        if (!target.isPasswordProvisoria()) {
            target.setDataExpiracaoPassword(null);
        }

        if (draft.isMfaEnabled()) {
            if (draft.getMfaSecret() == null || draft.getMfaSecret().isBlank()) {
                throw new IllegalArgumentException("MFA activo sem uma chave TOTP válida.");
            }
            target.setMfaEnabled(true);
            target.setMfaSecret(draft.getMfaSecret());
            target.setMfaRecoveryCodes(draft.getMfaRecoveryCodes());
        } else {
            target.setMfaEnabled(false);
            target.setMfaSecret(null);
            target.setMfaRecoveryCodes(null);
        }

        if (isNew) {
            target.setFailedAttempts(0);
            target.setLockoutEnd(null);
            target.setUltimoAcesso(null);
            target.setUltimoIpLogin(null);
        }

        User saved = userRepository.save(target);

        auditService.logAction(
                managedActor,
                null,
                isNew ? AuditLog.AuditActionType.CREATE : AuditLog.AuditActionType.UPDATE,
                "USER",
                String.valueOf(saved.getId()),
                (isNew ? "Criado" : "Actualizado") + " utilizador " + saved.getEmail(),
                oldSnapshot,
                userSnapshot(saved),
                MODULE,
                sourceIp,
                null,
                null,
                false,
                AuditLog.AGTComplianceLevel.NORMAL
        );

        return saved;
    }

    @Transactional
    public User alterarEstado(User actor, Long targetUserId, boolean active, String sourceIp) {
        require(actor, "EDITAR");
        User managedActor = managedActor(actor);
        User target = target(targetUserId);

        if (isSelf(managedActor, target) && !active) {
            throw new IllegalArgumentException("A própria conta não pode ser desactivada.");
        }
        if (target.isSuperadmin() && !managedActor.isSuperadmin()) {
            throw new SecurityException("Só um Superadministrador pode alterar o estado de outro Superadministrador.");
        }

        boolean previous = Boolean.TRUE.equals(target.getActive());
        target.setActive(active);
        User saved = userRepository.save(target);

        if (!active) {
            userSessionRepository.deleteAllByUsername(target.getNome());
        }

        auditService.logAction(
                managedActor,
                null,
                AuditLog.AuditActionType.UPDATE,
                "USER",
                String.valueOf(saved.getId()),
                (active ? "Conta activada: " : "Conta desactivada: ") + saved.getEmail(),
                snapshotState(previous),
                snapshotState(active),
                MODULE,
                sourceIp,
                null,
                null,
                false,
                AuditLog.AGTComplianceLevel.NORMAL
        );
        return saved;
    }

    @Transactional
    public User desbloquear(User actor, Long targetUserId, String sourceIp) {
        require(actor, "EDITAR");
        User managedActor = managedActor(actor);
        User target = target(targetUserId);

        if (target.isSuperadmin() && !managedActor.isSuperadmin()) {
            throw new SecurityException("Só um Superadministrador pode desbloquear outro Superadministrador.");
        }

        target.setFailedAttempts(0);
        target.setLockoutEnd(null);
        User saved = userRepository.save(target);

        auditService.logAction(
                managedActor,
                null,
                AuditLog.AuditActionType.UPDATE,
                "USER",
                String.valueOf(saved.getId()),
                "Bloqueio removido: " + saved.getEmail(),
                null,
                snapshotState(saved),
                MODULE,
                sourceIp,
                null,
                null,
                false,
                AuditLog.AGTComplianceLevel.NORMAL
        );
        return saved;
    }

    @Transactional
    public long terminarSessoes(User actor, Long targetUserId, String sourceIp) {
        require(actor, "EDITAR");
        User managedActor = managedActor(actor);
        User target = target(targetUserId);

        if (target.isSuperadmin() && !managedActor.isSuperadmin()) {
            throw new SecurityException("Só um Superadministrador pode terminar sessões de outro Superadministrador.");
        }

        long deleted = userSessionRepository.deleteAllByUsername(target.getNome());
        auditService.logAction(
                managedActor,
                null,
                AuditLog.AuditActionType.LOGOUT,
                "USER_SESSION",
                String.valueOf(target.getId()),
                "Sessões terminadas administrativamente: " + target.getEmail() + " (" + deleted + ")",
                null,
                snapshotState(target),
                MODULE,
                sourceIp,
                null,
                null,
                false,
                AuditLog.AGTComplianceLevel.NORMAL
        );
        return deleted;
    }

    /**
     * Mantém rastreabilidade e integridade referencial: remover uma conta administrativa
     * é tratado como arquivamento (soft delete), não como DELETE físico.
     */
    @Transactional
    public User arquivar(User actor, Long targetUserId, String sourceIp) {
        return alterarEstado(actor, targetUserId, false, sourceIp);
    }

    private Set<PerfilAcesso> validateProfiles(Set<PerfilAcesso> requested, Empresa empresa) {
        if (requested == null || requested.isEmpty()) return new LinkedHashSet<>();

        List<Long> ids = requested.stream()
                .filter(p -> p != null && p.getId() != null)
                .map(PerfilAcesso::getId)
                .toList();

        List<PerfilAcesso> loaded = perfilRepository.findAllById(ids);
        if (loaded.size() != ids.size()) {
            throw new IllegalArgumentException("Um ou mais perfis seleccionados já não existem.");
        }

        Set<PerfilAcesso> result = new LinkedHashSet<>();
        for (PerfilAcesso p : loaded) {
            if (!Boolean.TRUE.equals(p.getActivo())) {
                throw new IllegalArgumentException("O perfil " + p.getCodigo() + " está inactivo.");
            }
            if (p.getEmpresa() != null && (empresa == null || !p.getEmpresa().getId().equals(empresa.getId()))) {
                throw new IllegalArgumentException("O perfil " + p.getCodigo() + " não pertence à empresa seleccionada.");
            }
            result.add(p);
        }
        return result;
    }

    private User target(Long id) {
        if (id == null) throw new IllegalArgumentException("Utilizador de destino inválido.");
        return userRepository.findByIdWithPerfis(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));
    }

    private void require(User actor, String operation) {
        if (actor == null || actor.getId() == null) {
            throw new SecurityException("Sessão administrativa inválida.");
        }

        User managed = managedActor(actor);
        if (!Boolean.TRUE.equals(managed.getActive())) {
            throw new SecurityException("A conta administrativa está inactiva.");
        }

        if (managed.isSuperadmin() || managed.getRole() == Role.ADMIN) return;

        try {
            PermissaoPerfil.Operacao op = PermissaoPerfil.Operacao.valueOf(
                    "REMOVER".equalsIgnoreCase(operation) ? "APAGAR" : operation
            );
            if (!securityService.hasPermission(managed, MODULE, RESOURCE, op)) {
                throw new SecurityException("Não possui permissão para gerir utilizadores.");
            }
        } catch (IllegalArgumentException ex) {
            throw new SecurityException("Operação de administração de utilizadores inválida.", ex);
        }
    }

    private User managedActor(User actor) {
        return userRepository.findById(actor.getId())
                .orElseThrow(() -> new SecurityException("Administrador da sessão não encontrado."));
    }

    private boolean isSelf(User a, User b) {
        return a != null && b != null && a.getId() != null && a.getId().equals(b.getId());
    }

    private String generateCode() {
        String code;
        do {
            code = "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (userRepository.existsByCodigoIgnoreCase(code));
        return code;
    }

    private String normalizeCode(String requested, String fallback) {
        String value = blankToNull(requested);
        return value == null ? fallback : value.toUpperCase();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEmail(String value) {
        return normalize(value).toLowerCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validatePassword(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("A palavra-passe deve ter pelo menos 8 caracteres.");
        }
        boolean upper = password.chars().anyMatch(Character::isUpperCase);
        boolean lower = password.chars().anyMatch(Character::isLowerCase);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        if (!upper || !lower || !digit) {
            throw new IllegalArgumentException(
                    "A palavra-passe deve conter maiúsculas, minúsculas e números."
            );
        }
    }

    private LinkedHashMap<String, Object> userSnapshot(User user) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("codigo", user.getCodigo());
        map.put("nome", user.getNome());
        map.put("email", user.getEmail());
        map.put("role", user.getRole() == null ? null : user.getRole().name());
        map.put("tipoConta", user.getTipoConta() == null ? null : user.getTipoConta().name());
        map.put("empresaId", user.getEmpresa() == null ? null : user.getEmpresa().getId());
        map.put("filialId", user.getFilial() == null ? null : user.getFilial().getId());
        map.put("activo", user.getActive());
        map.put("idioma", user.getIdioma());
        map.put("tema", user.getTema());
        map.put("departamento", user.getDepartamento());
        map.put("cargo", user.getCargo());
        map.put("mfaEnabled", user.isMfaEnabled());
        map.put("passwordProvisoria", user.isPasswordProvisoria());
        map.put("dataExpiracaoPassword", user.getDataExpiracaoPassword());
        map.put("failedAttempts", user.getFailedAttempts());
        map.put("lockoutEnd", user.getLockoutEnd());
        map.put("superadmin", user.isSuperadmin());
        return map;
    }

    private LinkedHashMap<String, Object> snapshotState(boolean active) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("activo", active);
        return map;
    }

    private LinkedHashMap<String, Object> snapshotState(User user) {
        return userSnapshot(user);
    }
}
