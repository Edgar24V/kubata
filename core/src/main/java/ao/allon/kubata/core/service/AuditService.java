package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço de Auditoria - rastreamento completo de operações.
 * Compatível com normas angolanas (SAFT-AO, AGT).
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public AuditLog logAction(User user, String username, AuditLog.AuditActionType actionType,
                              String entityType, String entityId, String entityDescription,
                              Object oldValues, Object newValues, String module,
                              String ipAddress, String userAgent, String sessionId,
                              Boolean saftRelevant, AuditLog.AGTComplianceLevel complianceLevel) {
        
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setUsername(username != null ? username : (user != null ? user.getNome() : "SYSTEM"));
        log.setActionType(actionType);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setEntityDescription(entityDescription);
        log.setModule(module);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setSessionId(sessionId);
        log.setTimestamp(LocalDateTime.now());
        log.setSuccess(true);
        log.setSaftRelevant(saftRelevant != null ? saftRelevant : false);
        log.setAgtComplianceLevel(complianceLevel != null ? complianceLevel : AuditLog.AGTComplianceLevel.NORMAL);
        
        try {
            if (oldValues != null) {
                log.setOldValues(objectMapper.writeValueAsString(oldValues));
            }
            if (newValues != null) {
                log.setNewValues(objectMapper.writeValueAsString(newValues));
            }
        } catch (JsonProcessingException e) {
            log.setOldValues(oldValues != null ? oldValues.toString() : null);
            log.setNewValues(newValues != null ? newValues.toString() : null);
        }
        
        // Gerar hash de integridade (simplificado)
        log.setHashIntegrity(generateIntegrityHash(log));
        
        return auditLogRepository.save(log);
    }

    @Transactional
    public AuditLog logError(User user, String username, AuditLog.AuditActionType actionType,
                              String entityType, String entityId, String errorMessage,
                              String module, String ipAddress) {
        
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setUsername(username != null ? username : (user != null ? user.getNome() : "SYSTEM"));
        log.setActionType(actionType);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setModule(module);
        log.setIpAddress(ipAddress);
        log.setTimestamp(LocalDateTime.now());
        log.setSuccess(false);
        log.setErrorMessage(errorMessage);
        log.setSaftRelevant(false);
        log.setAgtComplianceLevel(AuditLog.AGTComplianceLevel.NORMAL);
        
        return auditLogRepository.save(log);
    }

    // Métodos convenientes para operações comuns

    @Transactional
    public void logCreate(User user, Object entity, String entityType, String entityId,
                          String description, String module, boolean saftRelevant) {
        logAction(user, null, AuditLog.AuditActionType.CREATE, entityType, entityId,
                description, null, entity, module, null, null, null, saftRelevant,
                saftRelevant ? AuditLog.AGTComplianceLevel.HIGH : AuditLog.AGTComplianceLevel.NORMAL);
    }

    @Transactional
    public void logUpdate(User user, Object oldEntity, Object newEntity, String entityType,
                          String entityId, String description, String module, boolean saftRelevant) {
        logAction(user, null, AuditLog.AuditActionType.UPDATE, entityType, entityId,
                description, oldEntity, newEntity, module, null, null, null, saftRelevant,
                saftRelevant ? AuditLog.AGTComplianceLevel.HIGH : AuditLog.AGTComplianceLevel.NORMAL);
    }

    @Transactional
    public void logDelete(User user, Object entity, String entityType, String entityId,
                          String description, String module, boolean saftRelevant) {
        logAction(user, null, AuditLog.AuditActionType.DELETE, entityType, entityId,
                description, entity, null, module, null, null, null, saftRelevant,
                saftRelevant ? AuditLog.AGTComplianceLevel.HIGH : AuditLog.AGTComplianceLevel.NORMAL);
    }

    @Transactional
    public void logLogin(User user, String ipAddress, String userAgent, String sessionId, boolean success) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setUsername(user != null ? user.getNome() : "UNKNOWN");
        log.setActionType(AuditLog.AuditActionType.LOGIN);
        log.setModule("AUTH");
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setSessionId(sessionId);
        log.setTimestamp(LocalDateTime.now());
        log.setSuccess(success);
        log.setSaftRelevant(false);
        log.setAgtComplianceLevel(AuditLog.AGTComplianceLevel.HIGH);
        log.setHashIntegrity(generateIntegrityHash(log));
        auditLogRepository.save(log);
    }

    @Transactional
    public void logLogout(User user, String sessionId) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setUsername(user != null ? user.getNome() : "UNKNOWN");
        log.setActionType(AuditLog.AuditActionType.LOGOUT);
        log.setModule("AUTH");
        log.setSessionId(sessionId);
        log.setTimestamp(LocalDateTime.now());
        log.setSuccess(true);
        log.setSaftRelevant(false);
        log.setAgtComplianceLevel(AuditLog.AGTComplianceLevel.NORMAL);
        log.setHashIntegrity(generateIntegrityHash(log));
        auditLogRepository.save(log);
    }

    @Transactional
    public void logSaftExport(User user, String period, String fileName, boolean success, String errorMessage) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setUsername(user != null ? user.getNome() : "SYSTEM");
        log.setActionType(AuditLog.AuditActionType.SAFT_EXPORT);
        log.setEntityType("SAFT");
        log.setEntityId(period);
        log.setEntityDescription(fileName);
        log.setModule("SAFT");
        log.setTimestamp(LocalDateTime.now());
        log.setSuccess(success);
        log.setErrorMessage(errorMessage);
        log.setSaftRelevant(true);
        log.setAgtComplianceLevel(AuditLog.AGTComplianceLevel.CRITICAL);
        log.setHashIntegrity(generateIntegrityHash(log));
        auditLogRepository.save(log);
    }

    // Consultas

    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findByOrderByTimestampDesc(pageable);
    }

    public Page<AuditLog> search(Long userId, AuditLog.AuditActionType actionType, String entityType,
                                  LocalDateTime startDate, LocalDateTime endDate, Boolean saftRelevant,
                                  Pageable pageable) {
        return auditLogRepository.search(userId, actionType, entityType, startDate, endDate, saftRelevant, pageable);
    }

    public List<AuditLog> findByEntity(String entityType, String entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
    }

    public List<AuditLog> findSaftRelevant() {
        return auditLogRepository.findBySaftRelevantTrueOrderByTimestampDesc();
    }

    public Map<String, Long> getStatisticsLast30Days() {
        LocalDateTime start = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        List<Object[]> counts = auditLogRepository.countByActionTypeSince(start);
        Map<String, Long> stats = new HashMap<>();
        for (Object[] row : counts) {
            stats.put(((AuditLog.AuditActionType) row[0]).name(), (Long) row[1]);
        }
        return stats;
    }

    public Map<String, Long> getDailyStatistics(LocalDateTime start, LocalDateTime end) {
        List<Object[]> counts = auditLogRepository.countByDay(start, end);
        Map<String, Long> stats = new HashMap<>();
        for (Object[] row : counts) {
            stats.put(row[0].toString(), (Long) row[1]);
        }
        return stats;
    }

    // Geração de hash de integridade (simplificado)
    private String generateIntegrityHash(AuditLog log) {
        String data = log.getUsername() + log.getActionType() + log.getTimestamp() + log.getEntityType() + log.getEntityId();
        return Integer.toHexString(data.hashCode());
    }
}
