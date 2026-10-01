package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserDevice;
import ao.allon.kubata.core.repository.AuditLogRepository;
import ao.allon.kubata.core.repository.UserDeviceRepository;
import ao.allon.kubata.core.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserDeviceService {

    private static final String MODULE = "ADMINISTRATOR";
    private static final String RESOURCE = "UTILIZADORES";

    private final UserDeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final SecurityService securityService;
    private final AuditService auditService;

    public UserDeviceService(UserDeviceRepository deviceRepository,
                             UserRepository userRepository,
                             SecurityService securityService,
                             AuditService auditService) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.securityService = securityService;
        this.auditService = auditService;
    }

    /**
     * Regista ou actualiza o dispositivo utilizado num login bem-sucedido.
     * Não depende de autorização administrativa porque é parte do fluxo interno de autenticação.
     */
    @Transactional
    public UserDevice registerLoginDevice(User user,
                                          String workstation,
                                          String ip,
                                          String platform) {
        if (user == null || user.getId() == null) {
            return null;
        }

        String normalizedWorkstation = safe(workstation, "STATION-01");
        String normalizedPlatform = safe(platform, "KUBATA DESKTOP");
        String key = deviceKey(normalizedWorkstation, normalizedPlatform);

        User managed = userRepository.findById(user.getId()).orElse(user);
        UserDevice device = deviceRepository.findByUserAndDeviceKey(managed, key)
                .orElseGet(() -> UserDevice.builder()
                        .user(managed)
                        .deviceKey(key)
                        .deviceName(normalizedWorkstation)
                        .deviceType("DESKTOP")
                        .platform(normalizedPlatform)
                        .firstSeen(LocalDateTime.now())
                        .lastSeen(LocalDateTime.now())
                        .build());

        boolean isNew = device.getId() == null;
        device.setDeviceName(normalizedWorkstation);
        device.setDeviceType("DESKTOP");
        device.setPlatform(normalizedPlatform);
        device.setLastSeen(LocalDateTime.now());
        device.setLastIp(ip);
        device.setRevokedAt(null);
        device.setActive(true);

        UserDevice saved = deviceRepository.save(device);

        if (isNew) {
            auditService.logAction(
                    managed,
                    null,
                    AuditLog.AuditActionType.CREATE,
                    "USER_DEVICE",
                    String.valueOf(saved.getId()),
                    "Novo dispositivo reconhecido para " + managed.getEmail(),
                    null,
                    deviceSnapshot(saved),
                    "AUTH",
                    ip,
                    normalizedPlatform,
                    null,
                    false,
                    AuditLog.AGTComplianceLevel.NORMAL
            );
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<UserDevice> listar(User actor, Long targetUserId) {
        User target = loadAuthorizedTarget(actor, targetUserId, "VER");
        return deviceRepository.findByUserOrderByLastSeenDesc(target);
    }

    @Transactional
    public UserDevice revogar(User actor, Long deviceId, String ip, String reason) {
        require(actor, "EDITAR");
        User managedActor = managedActor(actor);

        UserDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Dispositivo não encontrado."));

        if (!canManageTarget(managedActor, device.getUser())) {
            throw new SecurityException("Não possui autorização para gerir este dispositivo.");
        }

        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.length() < 3) {
            throw new IllegalArgumentException("Indique o motivo da revogação do dispositivo.");
        }

        java.util.Map<String, Object> oldSnapshot = deviceSnapshot(device);
        device.setActive(false);
        device.setTrusted(false);
        device.setRevokedAt(LocalDateTime.now());
        device.setNotes(normalizedReason);
        UserDevice saved = deviceRepository.save(device);

        auditService.logAction(
                managedActor,
                null,
                AuditLog.AuditActionType.UPDATE,
                "USER_DEVICE",
                String.valueOf(saved.getId()),
                "Dispositivo revogado: " + safe(saved.getDeviceName(), saved.getDeviceKey()),
                oldSnapshot,
                deviceSnapshot(saved),
                MODULE,
                ip,
                null,
                null,
                false,
                AuditLog.AGTComplianceLevel.NORMAL
        );
        return saved;
    }

    @Transactional
    public UserDevice setTrusted(User actor,
                                 Long deviceId,
                                 boolean trusted,
                                 String ip) {
        require(actor, "EDITAR");
        User managedActor = managedActor(actor);

        UserDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Dispositivo não encontrado."));

        if (!canManageTarget(managedActor, device.getUser())) {
            throw new SecurityException("Não possui autorização para gerir este dispositivo.");
        }

        java.util.Map<String, Object> oldSnapshot = deviceSnapshot(device);
        device.setTrusted(trusted);
        if (trusted) {
            device.setActive(true);
            device.setRevokedAt(null);
        }
        UserDevice saved = deviceRepository.save(device);

        auditService.logAction(
                managedActor,
                null,
                AuditLog.AuditActionType.UPDATE,
                "USER_DEVICE",
                String.valueOf(saved.getId()),
                (trusted ? "Dispositivo confiável: " : "Dispositivo deixou de ser confiável: ")
                        + safe(saved.getDeviceName(), saved.getDeviceKey()),
                oldSnapshot,
                deviceSnapshot(saved),
                MODULE,
                ip,
                null,
                null,
                false,
                AuditLog.AGTComplianceLevel.NORMAL
        );
        return saved;
    }

    private User loadAuthorizedTarget(User actor, Long targetUserId, String operation) {
        require(actor, operation);
        User managedActor = managedActor(actor);
        if (targetUserId == null) {
            throw new IllegalArgumentException("Utilizador de destino inválido.");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));

        if (!canManageTarget(managedActor, target)) {
            throw new SecurityException("Não possui autorização para gerir este utilizador.");
        }
        return target;
    }

    private boolean canManageTarget(User actor, User target) {
        if (actor == null || target == null) return false;
        if (actor.isSuperadmin()) return true;
        if (target.isSuperadmin() && !actor.isSuperadmin()) return false;
        return true;
    }

    private void require(User actor, String operation) {
        if (actor == null || actor.getId() == null) {
            throw new SecurityException("Sessão administrativa inválida.");
        }
        User managed = userRepository.findById(actor.getId())
                .orElseThrow(() -> new SecurityException("Sessão administrativa inválida."));
        if (!Boolean.TRUE.equals(managed.getActive())) {
            throw new SecurityException("A conta administrativa está inactiva.");
        }
        if (!managed.isSuperadmin()
                && managed.getRole() != Role.ADMIN
                && !securityService.hasPermission(
                        managed,
                        MODULE,
                        RESOURCE,
                        ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.valueOf(operation))) {
            throw new SecurityException("Não possui permissão para gerir dispositivos de utilizadores.");
        }
    }

    private User managedActor(User actor) {
        return userRepository.findById(actor.getId())
                .orElseThrow(() -> new SecurityException("Administrador da sessão não encontrado."));
    }

    private String deviceKey(String workstation, String platform) {
        String raw = workstation.trim().toLowerCase() + "|" + platform.trim().toLowerCase();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte b : hash) result.append(String.format("%02x", b));
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível no runtime.", ex);
        }
    }

    private java.util.Map<String, Object> deviceSnapshot(UserDevice d) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("userId", d.getUser() == null ? null : d.getUser().getId());
        map.put("deviceName", d.getDeviceName());
        map.put("deviceType", d.getDeviceType());
        map.put("platform", d.getPlatform());
        map.put("lastIp", d.getLastIp());
        map.put("trusted", d.isTrusted());
        map.put("active", d.getActive());
        map.put("revokedAt", d.getRevokedAt());
        return map;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
