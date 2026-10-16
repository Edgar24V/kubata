# Padrão oficial de modais e formulários do Kubata

## Regra
Todos os módulos JavaFX do Kubata devem reutilizar o gestor comum:

`ao.allon.kubata.core.ui.modal.ModalManager`

Não devem criar diálogos de interface directamente com `new Alert(...)`, `new Dialog(...)`, `new ChoiceDialog(...)` ou `new TextInputDialog(...)`, nem bloquear o fluxo da interface com `showAndWait()`. Isto inclui formulários CRUD, confirmações, mensagens de validação, operações administrativas, relatórios e o PDV.

Excepções: os selectores nativos de ficheiros/pastas (`FileChooser` / `DirectoryChooser`) podem continuar a ser usados para interagir com o sistema operativo. Não são substitutos dos modais de negócio.

## Mensagens
Nas classes que recebem o gestor por injecção:

```java
modalManager.info("Informação", "Operação concluída.");
modalManager.success("Sucesso", "Dados guardados.");
modalManager.warning("Validar dados", "Seleccione um registo.");
modalManager.danger("Erro", "Não foi possível concluir a operação.");
modalManager.showErrorModal("Erro", "A operação falhou.", exception);
```

Mensagens em componentes comuns sem injecção directa podem usar `ModalManager.getActiveInstance()` e confirmar `hasRoot()` antes de chamar o gestor. Se a interface ainda não estiver inicializada, o código deve manter a informação técnica no log em vez de tentar abrir uma janela JavaFX sem raiz.

## Confirmações assíncronas
Não devolver `boolean` de uma função de confirmação síncrona. A acção seguinte deve ser passada como callback:

```java
modalManager.showConfirm(
        "Excluir registo",
        "Tem a certeza de que pretende excluir este registo?",
        () -> {
            service.delete(selected.getId());
            refresh();
        }
);
```

No Faturação, `AlertUtils.showConfirmationAsync(title, message, onConfirm)` delega no gestor partilhado.

## Formulários
Construir os campos JavaFX numa `Node` (por exemplo, `GridPane`, `VBox` ou `Form`) e apresentar o conteúdo no `ModalManager`. A validação deve correr no callback de confirmar, sem executar a operação de gravação caso os valores sejam inválidos.

```java
modalManager.showModal(
        form,
        new ModalManager.ModalConfig()
                .title("Editar registo")
                .subtitle("Preencha os campos obrigatórios")
                .size(620, 440)
                .withConfirmButtons("Guardar", "Cancelar")
                .onConfirm(() -> {
                    if (!validarCampos()) {
                        modalManager.warning("Validar dados", "Verifique os campos.");
                        return;
                    }
                    service.save(entity);
                    refresh();
                })
);
```

Para os ecrãs de Faturação que usam `ModalService`, utilizar o construtor de compatibilidade, que encaminha para o gestor do Core. Para formulários com validação que precisa de manter o modal aberto, preferir `AlertUtils.showForm(...)` ou botões personalizados no rodapé com `ModalConfig.footerNode(...)`.

## Inicialização e raiz
O `ModalManager` deve ter como raiz o `StackPane` da cena principal, definido por `modalManager.setRoot(root)`. O módulo Faturação pode inicializar a raiz através de `ModalService.initialize(root)`, que também delega no Core. Todos os modais devem ser criados na mesma raiz para manter o overlay, o comportamento de empilhamento e o estilo uniforme.

## Revisão de código
Antes de integrar alterações de UI, pesquisar e rever:
- `new Alert(`
- `new Dialog<`
- `new ChoiceDialog`
- `new TextInputDialog`
- `showAndWait()`
- confirmações booleanas síncronas

Cada ocorrência deve ser migrada para o gestor comum ou ter uma justificação explícita de sistema operativo/integração nativa. Os estilos e os controlos de modal não devem ser reimplementados localmente em cada módulo.
