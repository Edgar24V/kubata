package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SessionManagerRoleTest {

    @Test
    public void adminHasAccessToEverything() {
        SessionManager sm = new SessionManager();
        User admin = new User();
        admin.setNome("Administrador");
        admin.setRole(Role.ADMIN);
        sm.login(admin);
        assertTrue(sm.hasAccess("FATURACAO","Ver"));
        assertTrue(sm.hasAccess("ESTOQUE","Ver"));
        assertTrue(sm.hasAccess("SAFT","Exportar"));
    }
}

