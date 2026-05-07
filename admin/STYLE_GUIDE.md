# 🟢 Guia de Estilos — Kubata Admin

Este guia documenta a padronização visual do módulo Admin, baseada no design corporativo "Excel 365 Verde".

## 🎨 Paleta de Cores (Variáveis CSS)

Todas as cores são definidas no seletor `.root` do ficheiro `admin.css`.

| Variável | Hex | Uso |
| :--- | :--- | :--- |
| `-kubata-green-dark` | `#1E6B3C` | Cabeçalhos de diálogos, títulos de tabelas. |
| `-kubata-green` | `#217346` | **Cor Primária**. Barra de Título, Ribbon, Botões Primários. |
| `-kubata-green-medium` | `#2E7D32` | Hover em botões primários. |
| `-kubata-green-light` | `#4CAF50` | Foco de campos, acentos visuais. |
| `-kubata-green-pale` | `#E8F5E9` | Fundos de seleção em tabelas, hover no Ribbon. |
| `-kubata-green-border` | `#A5D6A7` | Bordas suaves e separadores. |

## 🏗️ Estrutura da Interface

### 1. Barra de Título (`CustomTitleBar`)
- **Fundo**: `-kubata-green`.
- **Botões de Controlo**: Transparentes, ícones IKONLI (`MaterialDesignW`).
- **Hover Fechar**: `#E81123` (Padrão Windows).
- **Pesquisa**: Campo arredondado com opacidade reduzida.

### 2. Ribbon (`RibbonBar`)
- **Fundo**: `-kubata-green` (Idêntico à Barra de Título).
- **Abas**: Texto branco com 85% de opacidade. Selecionada fica com fundo branco e texto verde escuro.
- **Botões**: Ícones com cor `-kubata-green`.

### 3. Controlos Comuns
- **Botões Padrão**: Cinza suave com bordas definidas.
- **Botões Primários**: Classe `.accent` ou `.button-primary`.
- **Botões de Estado**: `.success` (Verde), `.danger` (Vermelho).
- **Estilos de Borda**: `.outlined` (Apenas borda), `.flat` (Sem borda/fundo).
- **Botões de Ícone**: Classe `.button-icon`. Ideal para botões sem texto. Tamanho fixo 32x32px.
- **Tabelas (`TableView`)**: 
    - Design profissional com bordas arredondadas (8px) e sombra sutil.
    - Cabeçalhos modernos com altura de 42px e texto em `-kubata-green-dark`.
    - Linhas alternadas (zebra stripes) para legibilidade.
    - Altura de linha otimizada (38px).
    - Seleção em `-kubata-green` com texto branco.
- **Tabelas Avançadas (`AdvancedTableView`)**:
    - Suporte a edição direta (TextField, ComboBox, CheckBox, DatePicker).
    - Validação visual em tempo real (borda vermelha em caso de erro).
    - Menu de contexto (Botão direito) para copiar, eliminar e exportar.
    - Exportação nativa para CSV.
- **ScrollBars**: Design minimalista e arredondado, sem botões de seta, integrados ao tema.
- **Campos de Texto**: Foco com borda verde de 2px.

## 🌗 Temas (Claro/Escuro)

O sistema suporta troca de temas através da classe `.dark-theme` no root.
As cores das tabelas e controlos adaptam-se automaticamente usando variáveis mapeadas.

## 🛠️ Como Manter a Consistência

1. **Sempre use as variáveis**: Evite usar hex codes diretamente nos ficheiros FXML ou Java. Utilize `-kubata-green`, etc.
2. **Classes de Botão**: Use `btn.getStyleClass().add("button-primary")` para ações principais.
3. **Ícones**: Utilize preferencialmente o pack `MaterialDesign2` do Ikonli.

---
*Atualizado em: 28 de Março de 2026*
