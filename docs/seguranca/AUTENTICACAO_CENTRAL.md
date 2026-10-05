# Autenticação Central da Plataforma Kubata

## Objectivo

O Kubata adopta um único modelo de identidade para as aplicações desktop da plataforma.

A autenticação é responsabilidade do Core e os clientes desktop reutilizam o mesmo fluxo:

1. Email e palavra-passe.
2. MFA TOTP ou código de recuperação, quando exigido.
3. Políticas do Perfil de Segurança Individual.
4. Validação de empresa/contexto.
5. Criação de uma UserSession exacta para a instância.
6. Autorização da aplicação solicitada.
7. Entrada na aplicação.

## Separação de responsabilidades

- AuthService: autenticação, password, MFA, bloqueio, expiração e sessão.
- AcessoService: decisão de acesso ao módulo com base em RBAC + Perfil de Segurança.
- UserSecurityProfileService: restrições individuais da conta comum.
- SecurityService: autorização granular para operações e recursos.
- CentralLoginController: UI JavaFX reutilizável por todos os clientes desktop.

O Perfil de Segurança Individual nunca concede permissões. Ele restringe as permissões já atribuídas pelo RBAC.

## Contexto de aplicação

O mesmo utilizador pode iniciar sessão em diferentes aplicações sem criar identidades diferentes.

Exemplos:

- ADMINISTRATOR
- RH
- FATURACAO
- INVENTARIO
- VENDAS
- COMPRAS
- FINANCEIRO
- CONTABILIDADE
- FISCAL

Quando uma aplicação chama AuthService.authenticateForApplication(...), o Core:

- autentica a identidade;
- valida a política de segurança;
- verifica se a conta pode abrir o módulo;
- encerra a sessão criada quando o acesso ao módulo é recusado;
- grava o contexto na sessão (ex.: KUBATA RH).

## Administrator

O contexto ADMINISTRATOR exige ADMIN ou Superadmin.

Contas ADMIN/Superadmin continuam fora do Perfil de Segurança Individual dos utilizadores comuns, preservando o fluxo administrativo.

## RH e restantes aplicações

O RH deixa de arrancar directamente no dashboard. A aplicação abre primeiro a mesma tela central de login usada pelo Administrator e pela Faturação.

Depois do login:

- o User autenticado passa para o MainController;
- a sessão real fica associada ao utilizador;
- o botão Sair termina a sessão;
- o botão X termina a sessão exacta antes de fechar a aplicação.

O mesmo componente pode ser reutilizado por futuras aplicações desktop.

## Segurança

A validação de acesso à aplicação é feita no servidor/Core. Desactivar um botão na interface não é considerado uma barreira de segurança.

Toda entrada rejeitada por falta de acesso ao módulo é registada na auditoria.
