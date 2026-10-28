package ao.allon.kubata.core.ui.login;

import org.kordamp.ikonli.feather.Feather;

public record LoginViewConfig(
        String applicationCode,
        String windowTitle,
        String productSubtitle,
        String description,
        String formSubtitle,
        String loginButtonText,
        String versionLabel,
        String preferenceNode,
        Feather brandingIcon,
        Copy copy
) {
    /**
     * Textos apresentados pelo ecrã. Vive aqui para que a view não contenha
     * literais e para que uma aplicação possa reescrever a sua própria redacção.
     */
    public record Copy(
            String windowSubtitle,
            String wordmark,
            String securityCardTitle,
            String securityCardText,
            String formTitle,
            String helpLabel,
            String emailLabel,
            String emailPrompt,
            String emailSavedHint,
            String passwordLabel,
            String passwordPrompt,
            String showPasswordLabel,
            String hidePasswordLabel,
            String mfaLabel,
            String mfaHintTotp,
            String mfaHintRecovery,
            String mfaPromptTotp,
            String mfaPromptRecovery,
            String recoveryModeLinkLabel,
            String totpModeLinkLabel,
            String rememberEmailLabel,
            String recoverAccessLabel,
            String footerNote,
            String statusChip,
            String maintenanceNotice
    ) {
        public static Copy defaults() {
            return new Copy(
                    "Acesso seguro",
                    "KUBATA",
                    "Camada de segurança",
                    "Bloqueio automático, expiração de credenciais, MFA "
                            + "e auditoria administrativa.",
                    "Iniciar sessão",
                    "Ajuda",
                    "Email",
                    "nome@empresa.ao",
                    "guardado neste computador",
                    "Palavra-passe",
                    "Introduza a sua palavra-passe",
                    "Mostrar",
                    "Ocultar",
                    "Código MFA",
                    "6 dígitos quando o MFA estiver activo",
                    "Código de recuperação de uso único",
                    "Código de 6 dígitos",
                    "ABCD-EFGH-JKLM",
                    "Usar código de recuperação",
                    "Usar código MFA",
                    "Lembrar email",
                    "Recuperar acesso",
                    "Nunca partilhe a sua palavra-passe ou código MFA.",
                    "Autenticação protegida • MFA disponível • "
                            + "credenciais temporárias exigem troca",
                    "O sistema está temporariamente em manutenção."
            );
        }
    }

    public static LoginViewConfig administrator() {
        return new LoginViewConfig(
                "ADMINISTRATOR",
                "KUBATA Administrator",
                "Administrator",
                "Gestão centralizada de utilizadores, segurança, configuração e operação empresarial.",
                "Aceda à consola administrativa do Kubata.",
                "ENTRAR NO KUBATA",
                "KUBATA Administrator • v2.0.0",
                "ao.allon.kubata.admin.login",
                Feather.SHIELD,
                Copy.defaults()
        );
    }

    public static LoginViewConfig faturacao() {
        return new LoginViewConfig(
                "FATURACAO",
                "KUBATA Faturação",
                "Faturação",
                "Faturação, stock, clientes e operação comercial com as mesmas regras de acesso do Administrator.",
                "Aceda ao módulo de faturação do Kubata.",
                "ENTRAR NA FATURAÇÃO",
                "KUBATA Faturação • v1.0.0",
                "ao.allon.kubata.faturacao.login",
                Feather.FILE_TEXT,
                Copy.defaults()
        );
    }

    public static LoginViewConfig rh() {
        return new LoginViewConfig(
                "RH",
                "KUBATA Recursos Humanos",
                "Recursos Humanos",
                "Colaboradores, assiduidade e férias com utilizadores e permissões definidos no Administrator.",
                "Aceda ao módulo de recursos humanos do Kubata.",
                "ENTRAR NO RH",
                "KUBATA RH • v1.0.0",
                "ao.allon.kubata.rh.login",
                Feather.USERS,
                Copy.defaults()
        );
    }
}
