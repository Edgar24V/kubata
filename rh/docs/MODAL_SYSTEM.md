# Documentação Técnica: Sistema de Modais (ModalManager)

## Visão Geral
O `ModalManager` é um serviço centralizado para gestão de diálogos e janelas modais no módulo RH do sistema Kubata. Este sistema utiliza exclusivamente o tema **JMetro** e componentes nativos JavaFX.

## Estrutura do Modal (InternalModalBox)
O sistema utiliza o `InternalModalBox`, um componente desenvolvido internamente para garantir consistência visual e funcional:
- **Estilo**: Baseado em CSS neutro, harmonizado com o tema JMetro.
- **Acessibilidade (WCAG 2.1)**:
    - Suporte ao fecho via tecla `ESC`.
    - Focus Trapping: O foco é capturado e mantido dentro do modal enquanto este estiver aberto.
- **Responsividade**: Utiliza `StackPane` e `VBox` com crescimento dinâmico para se ajustar a diferentes resoluções.

## Overlay (JMetroModalPane)
Para exibir os modais sobre a interface principal, o sistema utiliza o `JMetroModalPane`, que cria um overlay semi-transparente bloqueando interações com o fundo.

## Instruções de Manutenção
1. **Estilos**: Edite a constante `DEFAULT_DIALOG_STYLE` no `ModalManager`.
2. **Novos tipos**: Adicione valores ao enum `ModalType` conforme necessário.

## Requisitos de Dependência
- `jfxtras-styles`: Necessário para o tema JMetro.
- `validatorfx`: Utilizado para validações em tempo real dentro dos modais.
