# Documentação Técnica: Sistema de Gestão de Perfis e Acessos (RBAC)

## 1. Arquitetura do Sistema
O sistema utiliza o padrão **RBAC (Role-Based Access Control)** com suporte a herança recursiva e auditoria em tempo real.

### Componentes Principais:
- **Perfil**: Define um agrupamento de permissões. Pode herdar permissões de um perfil "pai".
- **Permissão**: Ação granular vinculada a um módulo (ex: FATURACAO, STOCK).
- **AuditoriaAcesso**: Registo de todas as operações críticas (Login, Acesso Negado, Alteração de Perfil).
- **MFA (TOTP)**: Autenticação de dois fatores baseada em tempo para usuários administrativos.

## 2. Diagrama de Domínio (Simplificado)
- `User` -> `Perfil` (Muitos para Um)
- `Perfil` -> `Perfil` (Auto-relacionamento para Herança)
- `Perfil` -> `Permissao` (Muitos para Muitos)
- `User` -> `AuditoriaAcesso` (Um para Muitos)

## 3. Manual do Administrador (ADI)
O Administrador de Identidade pode:
1. **Criar Perfis**: Definir novos papéis no sistema.
2. **Atribuir Permissões**: Selecionar módulos e ações específicas para cada perfil.
3. **Gerir Herança**: Definir um perfil base para novos perfis (ex: Supervisor herda de Operador).
4. **Monitorar Auditoria**: Visualizar logs de acesso, IPs e tentativas de violação.
5. **Exportar Matriz**: Gerar relatórios em PDF com a configuração atual de cada perfil.

## 4. Segurança (OWASP Top 10)
- **A01:2021-Broken Access Control**: Mitigado através de validação centralizada no `AcessoService` e Spring Security.
- **A07:2021-Identification and Authentication Failures**: Implementado MFA e Bloqueio de conta (Rate Limiting).
- **A09:2021-Security Logging and Monitoring Failures**: Sistema de Auditoria detalhado com persistência em base de dados.

## 5. Performance e Escalabilidade
- Otimizado para 10.000 usuários através de:
  - Carregamento Eager de permissões no login.
  - Índices em `User.email` e `AuditoriaAcesso.timestamp`.
  - Cache de sessão via Spring Session (opcional para expansão).
