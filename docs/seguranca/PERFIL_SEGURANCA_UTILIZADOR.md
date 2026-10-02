# Perfil de Segurança do Utilizador

## Objectivo

O **Perfil de Segurança do Utilizador** é uma política individual aplicável apenas a contas comuns. A funcionalidade reforça a segurança sem substituir o modelo de autorização existente e sem alterar o fluxo de login administrativo.

## Modelo de autorização

A arquitectura do Kubata passa a ter três camadas bem definidas:

1. **Autenticação** — `AuthService` valida identidade, estado da conta, palavra-passe, MFA e criação da sessão.
2. **Autorização funcional (RBAC)** — `AcessoService`, `SecurityService`, `PerfilAcesso` e `PermissaoPerfil` determinam as funções que a conta pode executar.
3. **Política individual** — `UserSecurityProfile` aplica restrições adicionais por utilizador: login, horário, dias, IP, empresas, módulos, sessões, palavra-passe e limites/operações críticas.

O Perfil de Segurança **não concede permissões funcionais por si só**. Um módulo ou função só fica disponível quando já existe autorização no RBAC; a política individual pode reduzir esse acesso.

## Separação Administrador vs. utilizador comum

- `Role.ADMIN` e Superadmin são contas administrativas.
- A política individual desta tela não pode ser gravada para contas administrativas.
- A política individual é ignorada na resolução de `getEffectiveProfile` para contas ADMIN/Superadmin.
- A ação **Perfil de segurança** fica indisponível na UI quando a linha seleccionada representa ADMIN/Superadmin.
- O módulo `ADMINISTRATOR` não pode ser incluído numa política individual.
- A própria API de serviço valida novamente estas regras; esconder um botão na UI não é considerado uma barreira de segurança.

## Regras de segurança

### Login
- conta activa/inactiva;
- login explicitamente permitido ou bloqueado;
- MFA obrigatório;
- código de recuperação MFA;
- número máximo de tentativas;
- duração do bloqueio;
- horário de login;
- dias da semana;
- IPs/CIDR autorizados.

### Sessões
- timeout individual;
- limite de sessões simultâneas;
- expiração de sessões antigas conforme a política.

### Palavra-passe
- comprimento mínimo;
- maiúsculas;
- minúsculas;
- dígitos;
- símbolos;
- expiração;
- validação server-side através do serviço de política de palavra-passe.

### Organização
- empresas autorizadas;
- módulos como restrição adicional;
- operações críticas;
- limites financeiros por operação e por dia.

## Princípios de implementação

- Toda regra de autorização relevante deve existir no servidor.
- A UI apenas representa o estado e melhora a experiência; nunca é a única barreira.
- Alterações de política são auditadas.
- Perfis de acesso continuam a ser a origem das funções.
- A política individual não deve criar privilégios administrativos.
- Contas ADMIN/Superadmin devem manter o fluxo de login administrativo mesmo quando existam dados históricos de `UserSecurityProfile`.

## Critérios de aceitação

- Um utilizador comum pode receber uma política individual válida.
- O utilizador comum só pode entrar nos módulos permitidos pela política quando já tiver autorização RBAC.
- Uma função não pode ser criada/concedida apenas por marcar um módulo no Perfil de Segurança.
- Um ADMIN/Superadmin não pode ser alvo desta tela.
- Uma tentativa server-side de alterar a política de um ADMIN/Superadmin é rejeitada.
- Uma política existente de ADMIN/Superadmin não altera o perfil efectivo dessa conta.
- `ADMINISTRATOR` não pode ser atribuído numa política individual.
- As regras de IP, horário, MFA, empresas, sessões e limites financeiros continuam server-side.
- Alterações bem-sucedidas geram auditoria de configuração.

## Fluxo operacional

**Administrador autorizado → Gestão de Utilizadores → seleccionar utilizador comum → Perfil de Segurança → definir restrições → Guardar → validação server-side → auditoria → política aplicada no próximo/actual contexto de autenticação e autorização.**

## Não regressão do login Administrator

O login do Kubata Administrator continua a utilizar `AuthService`. A funcionalidade desta tela não cria um segundo mecanismo de login e não substitui a autenticação administrativa.

As políticas guardadas para utilizadores comuns não alteram contas ADMIN/Superadmin. Políticas de MFA que sejam provenientes de mecanismos administrativos/centrais continuam a ser tratadas pelos respectivos serviços; o isolamento descrito neste documento diz respeito especificamente ao `UserSecurityProfile` individual.
