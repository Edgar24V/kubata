package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.SessionTicket;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.exception.AuthenticationException;
import ao.allon.kubata.core.repository.SessionTicketRepository;
import ao.allon.kubata.core.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Emissão e consumo de bilhetes de sessão entre aplicações Kubata.
 *
 * <p>O bilhete é prova de um login já validado no Kubata Administrator, nunca uma credencial:
 * quem o recebe revalida sempre a autorização do utilizador antes de abrir a sessão local.</p>
 */
@Service
public class SessionTicketService {

    private final SessionTicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ModuleAccessService moduleAccessService;
    private final UserSecurityProfileService userSecurityProfileService;
    private final MaintenanceModeService maintenanceModeService;
    private final AcessoService acessoService;
    private final LaunchContext launchContext;

    public SessionTicketService(SessionTicketRepository ticketRepository,
                                UserRepository userRepository,
                                ModuleAccessService moduleAccessService,
                                UserSecurityProfileService userSecurityProfileService,
                                MaintenanceModeService maintenanceModeService,
                                AcessoService acessoService,
                                LaunchContext launchContext) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.moduleAccessService = moduleAccessService;
        this.userSecurityProfileService = userSecurityProfileService;
        this.maintenanceModeService = maintenanceModeService;
        this.acessoService = acessoService;
        this.launchContext = launchContext;
    }

    @Transactional
    public SessionTicket emitir(User emissor, String applicationCode, Long empresaId, Integer exercicioAno) {
        if (emissor == null || emissor.getId() == null) {
            throw new AuthenticationException("Sem sessão activa para emitir o bilhete.");
        }
        moduleAccessService.assertCanEnterApplication(emissor, applicationCode);
        if (empresaId != null && !userSecurityProfileService.isCompanyAllowed(emissor, empresaId)) {
            throw new AuthenticationException("Não tem acesso a esta empresa.");
        }

        ticketRepository.eliminarExpirados(LocalDateTime.now());

        LocalDateTime agora = LocalDateTime.now();
        SessionTicket ticket = new SessionTicket();
        ticket.setToken(UUID.randomUUID().toString());
        ticket.setUserId(emissor.getId());
        ticket.setApplicationCode(applicationCode);
        ticket.setEmpresaId(empresaId);
        ticket.setExercicioAno(exercicioAno);
        ticket.setIssuedByUserId(emissor.getId());
        ticket.setWorkstation(localWorkstation());
        ticket.setExpiresAt(agora.plusSeconds(SessionTicket.VALIDADE_SEGUNDOS));
        return ticketRepository.save(ticket);
    }

    /**
     * Consome o bilhete e devolve o utilizador, revalidando a autorização do destinatário.
     * O consumo é atómico: um segundo arranque com o mesmo token é recusado.
     */
    @Transactional
    public User consumir(String token, String applicationCodeEsperada, String ip) {
        if (token == null || token.isBlank()) {
            throw new AuthenticationException("Bilhete de sessão ausente.");
        }

        Optional<SessionTicket> encontrado = ticketRepository.findByToken(token.trim());
        if (encontrado.isEmpty()) {
            throw new AuthenticationException("Bilhete de sessão desconhecido.");
        }

        SessionTicket ticket = encontrado.get();
        LocalDateTime agora = LocalDateTime.now();

        if (ticket.getConsumedAt() != null) {
            throw new AuthenticationException("Bilhete de sessão já utilizado.");
        }
        if (ticket.getExpiresAt() == null || ticket.getExpiresAt().isBefore(agora)) {
            throw new AuthenticationException("Bilhete de sessão expirado.");
        }
        if (applicationCodeEsperada != null && !applicationCodeEsperada.isBlank()
                && !applicationCodeEsperada.equalsIgnoreCase(ticket.getApplicationCode())) {
            throw new AuthenticationException("Bilhete emitido para outra aplicação.");
        }

        User user = userRepository.findById(ticket.getUserId() == null ? -1L : ticket.getUserId())
                .orElseThrow(() -> new AuthenticationException("Utilizador do bilhete não existe."));

        if (!user.isEnabled()) {
            throw new AuthenticationException("Conta inativa. Contate o administrador.");
        }
        if (maintenanceModeService.isEnabled()
                && !(user.isSuperadmin() || user.getRole() == Role.ADMIN)) {
            throw new AuthenticationException(
                    "O sistema está temporariamente em manutenção. " + maintenanceModeService.getReason()
            );
        }

        // O bilhete substitui o pedido de credenciais, nunca a verificação de permissões.
        moduleAccessService.assertCanEnterApplication(user, ticket.getApplicationCode());
        if (ticket.getEmpresaId() != null && !userSecurityProfileService.isCompanyAllowed(user, ticket.getEmpresaId())) {
            throw new AuthenticationException("Não tem acesso a esta empresa.");
        }

        if (ticketRepository.consumir(ticket.getToken(), agora) != 1) {
            throw new AuthenticationException("Bilhete de sessão já utilizado.");
        }

        user.setUltimoAcesso(agora);
        user.setUltimoIpLogin(ip);
        userRepository.save(user);

        acessoService.registrarAuditoria(user, "OPEN_MODULE", ticket.getApplicationCode(), ip,
                "Sessão iniciada por bilhete do Kubata Administrator", true);

        if (ticket.getEmpresaId() != null) {
            launchContext.set(ticket.getEmpresaId(), ticket.getExercicioAno());
        }
        return user;
    }

    public Optional<SessionTicket> porToken(String token) {
        return token == null || token.isBlank()
                ? Optional.empty()
                : ticketRepository.findByToken(token.trim());
    }

    @Transactional
    public int limparExpirados() {
        return ticketRepository.eliminarExpirados(LocalDateTime.now());
    }

    private String localWorkstation() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return "KUBATA-POSTO";
        }
    }
}
