# Header Moderno - Kubata Faturação

## Estrutura e Principais Elementos
- Campo de pesquisa com validação e busca em tempo real.
- Centro de notificações com badge e lista expansível.
- Terminal de login com lembrete e recuperação de senha.
- Botão de alternância do menu (hambúrguer → X) com animações.

## APIs e Eventos
- Pesquisa: onGlobalSearch(String query) em MainController.
- Notificações: NotificationModel gerencia itens e estados (lido/não lido).
- Autenticação: SessionManager expõe login(String, Set<String>).

## Responsividade
- Quebras aplicadas no header e no menu lateral, com transições suaves.

## Remoções
- Botões legados de busca e sync foram removidos do header e substituídos por:
  - Campo de pesquisa completo.
  - Centro de notificações.

## Estilo
- Classes: app-header, app-sidenav, app-center, app-footer.
- Tokens do tema Atlantafx para cores, bordas e superfícies.

## Testes
- NotificationModelTest: estados lidos/não lidos.
- SearchValidatorTest: validação de entradas.
