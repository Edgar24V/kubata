package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserAccessPermission;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class AcessoService {

    @Autowired
    private ao.allon.kubata.core.repository.UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private ao.allon.kubata.core.repository.UserAccessPermissionRepository userAccessRepository;

    @Autowired
    private ao.allon.kubata.core.repository.PerfilAcessoRepository perfilRepository;

    @Autowired
    private ao.allon.kubata.core.repository.PermissaoPerfilRepository permissaoRepository;

    @Autowired
    private AuditService auditService;

    @Transactional
    public User salvarUsuario(User user, String rawPassword) {
        if (user.getId() == null) {
            if (userRepository.existsByEmail(user.getEmail())) {
                throw new IllegalArgumentException("Email já cadastrado.");
            }
            if (rawPassword == null || rawPassword.isBlank()) {
                throw new IllegalArgumentException("Senha é obrigatória para novos usuários.");
            }
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setPasswordChangedAt(LocalDateTime.now());
        } else {
            if (rawPassword != null && !rawPassword.isBlank()) {
                user.setPassword(passwordEncoder.encode(rawPassword));
                user.setPasswordChangedAt(LocalDateTime.now());
            }
        }
        return userRepository.save(user);
    }

    @Transactional
    public void excluirUsuario(User admin, User target) {
        userRepository.delete(target);
    }

    @Transactional(readOnly = true)
    public List<UserAccessPermission> listarAcessosUsuario(User user) {
        return userAccessRepository.findByUser(user);
    }

    @Transactional
    public void registrarAuditoria(User user, String operacao, String recurso, String ip, String detalhes, boolean sucesso) {
        AuditLog.AuditActionType type;
        try {
            type = AuditLog.AuditActionType.valueOf(operacao);
        } catch (Exception e) {
            type = AuditLog.AuditActionType.CONFIG_CHANGE;
        }

        if (sucesso) {
            auditService.logAction(user, null, type, recurso, null, detalhes, null, null, "CORE", ip, null, null, false, AuditLog.AGTComplianceLevel.NORMAL);
        } else {
            auditService.logError(user, null, type, recurso, null, detalhes, "CORE", ip);
        }
    }

    @Transactional
    public void salvarAcessosUsuario(Long userId, Map<String, Set<String>> modulosParaOpcoes) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        if (user.getRole() == Role.ADMIN) {
            // Administrador tem acesso total por definição; nenhuma permissão granular será gravada
            return;
        }
        userAccessRepository.deleteByUser_Id(userId);
        userAccessRepository.flush();
        List<UserAccessPermission> toSave = modulosParaOpcoes.entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(op -> {
                    UserAccessPermission uap = new UserAccessPermission();
                    uap.setUser(user);
                    uap.setModulo(e.getKey());
                    uap.setOpcao(op);
                    return uap;
                }))
                .collect(Collectors.toList());
        if (!toSave.isEmpty()) {
            userAccessRepository.saveAll(toSave);
        }
    }

    @Transactional(readOnly = true)
    public List<User> listarTodosUsuarios() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ao.allon.kubata.core.domain.PerfilAcesso> listarTodosPerfis() {
        return perfilRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ao.allon.kubata.core.domain.PerfilAcesso> listarPerfisPorEmpresa(ao.allon.kubata.core.domain.Empresa empresa) {
        if (empresa == null) return perfilRepository.findByEmpresaIsNull();
        return perfilRepository.findByEmpresa(empresa);
    }

    @Transactional
    public ao.allon.kubata.core.domain.PerfilAcesso salvarPerfil(ao.allon.kubata.core.domain.PerfilAcesso perfil) {
        return perfilRepository.save(perfil);
    }

    @Transactional
    public void excluirPerfil(ao.allon.kubata.core.domain.PerfilAcesso perfil) {
        if (Boolean.TRUE.equals(perfil.getSistema())) {
            throw new IllegalArgumentException("Perfis de sistema não podem ser removidos.");
        }
        perfilRepository.delete(perfil);
    }

    @Transactional
    public void salvarPermissoesPerfil(ao.allon.kubata.core.domain.PerfilAcesso perfil, List<ao.allon.kubata.core.domain.PermissaoPerfil> permissoes) {
        permissaoRepository.deleteByPerfil(perfil);
        permissaoRepository.flush();
        permissoes.forEach(p -> p.setPerfil(perfil));
        permissaoRepository.saveAll(permissoes);
    }

    @Transactional(readOnly = true)
    public boolean temAcesso(User user, String modulo, String recurso, ao.allon.kubata.core.domain.PermissaoPerfil.Operacao operacao) {
        if (user == null) return false;
        if (user.isSuperadmin()) return true;
        if (user.getRole() == Role.ADMIN) return true;

        // Verifica permissões diretas (legado)
        if (userAccessRepository.existsByUserAndModuloIgnoreCaseAndOpcaoIgnoreCase(user, modulo, recurso)) {
            return true;
        }

        // Verifica permissões via perfis
        if (user.getPerfis() != null) {
            return user.getPerfis().stream()
                .filter(p -> p != null && Boolean.TRUE.equals(p.getActivo()))
                .flatMap(p -> p.getPermissoes().stream())
                .anyMatch(pm ->
                    pm.getModulo().equalsIgnoreCase(modulo) &&
                    pm.getRecurso().equalsIgnoreCase(recurso) &&
                    pm.getOperacao() == operacao &&
                    Boolean.TRUE.equals(pm.getPermitido())
                );
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean temAcesso(User user, String modulo, String opcao) {
        // Sobrecarga para compatibilidade com código existente
        return temAcesso(user, modulo, opcao, ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.VER);
    }

    @Transactional
    public User alterarStatusUsuario(Long userId, boolean ativo) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        user.setActive(ativo);
        return userRepository.save(user);
    }

    @Transactional
    public void redefinirSenha(Long userId, String novaSenha) {
        if (novaSenha == null || novaSenha.isBlank()) {
            throw new IllegalArgumentException("Senha inválida.");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        user.setPassword(passwordEncoder.encode(novaSenha));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setFailedAttempts(0);
        user.setLockoutEnd(null);
        userRepository.save(user);
    }
}
