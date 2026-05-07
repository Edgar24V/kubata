# 🟢 Kubata Admin — Tema Verde (Estilo Excel 365)

Guia completo de implementação do tema verde para o módulo admin,
incluindo CSS global, barra de título personalizada com campo de pesquisa
e Ribbon actualizado.

---

## 📁 Estrutura de Ficheiros

```
src/main/resources/ao/allon/kubata/admin/ui/
├── styles/
│   ├── admin.css          ← CSS global do módulo (NOVO)
│   └── ribbon.css         ← CSS do Ribbon (ACTUALIZADO)
src/main/java/ao/allon/kubata/admin/ui/
├── component/
│   └── CustomTitleBar.java   ← Barra de título personalizada (NOVO)
└── util/
    └── ThemeManager.java     ← Actualizado para carregar os novos CSS
```

---

## 1. `admin.css` — CSS Global do Módulo Admin

Guarda em: `src/main/resources/ao/allon/kubata/admin/ui/styles/admin.css`

```css
/* ════════════════════════════════════════════════════════
   KUBATA ADMIN — Tema Verde Corporativo (Estilo Excel 365)
   Paleta principal: #1E6B3C (verde escuro) / #217346 / #E8F5E9
   ════════════════════════════════════════════════════════ */

/* ─── Variáveis de Cor ─────────────────────────────────── */
/*
    --kubata-green-dark   : #1E6B3C   (título, cabeçalhos)
    --kubata-green        : #217346   (primário, botões activos)
    --kubata-green-medium : #2E7D32   (hover states)
    --kubata-green-light  : #4CAF50   (accents, focus)
    --kubata-green-pale   : #E8F5E9   (fundos suaves)
    --kubata-green-border : #A5D6A7   (bordas suaves)
    --kubata-white        : #FFFFFF
    --kubata-gray-50      : #FAFAFA
    --kubata-gray-100     : #F5F5F5
    --kubata-gray-200     : #EEEEEE
    --kubata-gray-300     : #E0E0E0
    --kubata-gray-600     : #757575
    --kubata-gray-800     : #424242
    --kubata-gray-900     : #212121
*/

/* ─── Root / Scene ─────────────────────────────────────── */
.root {
    -fx-font-family: "Segoe UI", "Calibri", "Arial", sans-serif;
    -fx-font-size: 13px;
    -fx-background-color: #F5F5F5;
    -fx-accent: #217346;
    -fx-focus-color: #4CAF50;
    -fx-faint-focus-color: rgba(33,115,70,0.12);
    -fx-selection-bar: #217346;
    -fx-selection-bar-non-focused: #A5D6A7;
    -fx-text-background-color: #FFFFFF;
    -fx-text-inner-color: #212121;
}

/* ─── Janela Principal (BorderPane raiz) ───────────────── */
.main-window {
    -fx-background-color: #F5F5F5;
}

/* ─── Barra de Título Personalizada ────────────────────── */
.custom-title-bar {
    -fx-background-color: #1E6B3C;
    -fx-min-height: 40px;
    -fx-pref-height: 40px;
    -fx-max-height: 40px;
    -fx-padding: 0 8 0 0;
}

.title-bar-app-icon {
    -fx-padding: 0 6 0 12;
}

.title-bar-app-name {
    -fx-text-fill: rgba(255,255,255,0.90);
    -fx-font-size: 13px;
    -fx-font-family: "Segoe UI Semibold", "Segoe UI", "Calibri", sans-serif;
    -fx-font-weight: bold;
    -fx-padding: 0 16 0 4;
}

/* Campo de Pesquisa na Barra de Título */
.title-bar-search-box {
    -fx-background-color: rgba(255,255,255,0.15);
    -fx-background-radius: 20;
    -fx-border-color: rgba(255,255,255,0.25);
    -fx-border-radius: 20;
    -fx-border-width: 1;
    -fx-text-fill: #FFFFFF;
    -fx-prompt-text-fill: rgba(255,255,255,0.60);
    -fx-font-size: 12px;
    -fx-padding: 4 12 4 32;
    -fx-min-width: 220px;
    -fx-pref-width: 280px;
    -fx-max-width: 360px;
    -fx-min-height: 26px;
    -fx-pref-height: 26px;
}

.title-bar-search-box:focused {
    -fx-background-color: rgba(255,255,255,0.25);
    -fx-border-color: rgba(255,255,255,0.60);
}

.title-bar-search-icon {
    -fx-text-fill: rgba(255,255,255,0.70);
    -fx-font-size: 14px;
}

/* Info do utilizador na barra de título */
.title-bar-user-label {
    -fx-text-fill: rgba(255,255,255,0.85);
    -fx-font-size: 12px;
    -fx-padding: 0 8 0 8;
}

.title-bar-user-avatar {
    -fx-background-color: rgba(255,255,255,0.20);
    -fx-background-radius: 50%;
    -fx-text-fill: #FFFFFF;
    -fx-font-size: 11px;
    -fx-font-weight: bold;
    -fx-min-width: 28px;
    -fx-min-height: 28px;
    -fx-pref-width: 28px;
    -fx-pref-height: 28px;
    -fx-alignment: center;
    -fx-border-color: rgba(255,255,255,0.35);
    -fx-border-radius: 50%;
    -fx-border-width: 1;
    -fx-cursor: hand;
}

.title-bar-user-avatar:hover {
    -fx-background-color: rgba(255,255,255,0.30);
}

/* Botões de controlo da janela (minimizar, maximizar, fechar) */
.title-bar-control-btn {
    -fx-background-color: transparent;
    -fx-text-fill: rgba(255,255,255,0.80);
    -fx-font-size: 12px;
    -fx-min-width: 46px;
    -fx-pref-width: 46px;
    -fx-min-height: 40px;
    -fx-pref-height: 40px;
    -fx-background-radius: 0;
    -fx-border-color: transparent;
    -fx-cursor: hand;
    -fx-padding: 0;
}

.title-bar-control-btn:hover {
    -fx-background-color: rgba(255,255,255,0.15);
    -fx-text-fill: #FFFFFF;
}

.title-bar-close-btn {
    -fx-background-color: transparent;
    -fx-text-fill: rgba(255,255,255,0.80);
    -fx-font-size: 12px;
    -fx-min-width: 46px;
    -fx-pref-width: 46px;
    -fx-min-height: 40px;
    -fx-pref-height: 40px;
    -fx-background-radius: 0;
    -fx-border-color: transparent;
    -fx-cursor: hand;
    -fx-padding: 0;
}

.title-bar-close-btn:hover {
    -fx-background-color: #C42B1C;
    -fx-text-fill: #FFFFFF;
}

/* ─── Barra de Ferramentas de Acesso Rápido (QAT) ─────── */
.quick-access-toolbar {
    -fx-background-color: #217346;
    -fx-padding: 0 4 0 8;
    -fx-min-height: 26px;
    -fx-pref-height: 26px;
    -fx-spacing: 2;
}

.qat-button {
    -fx-background-color: transparent;
    -fx-text-fill: rgba(255,255,255,0.85);
    -fx-background-radius: 3;
    -fx-border-color: transparent;
    -fx-border-radius: 3;
    -fx-min-width: 22px;
    -fx-min-height: 22px;
    -fx-padding: 3 6 3 6;
    -fx-cursor: hand;
    -fx-font-size: 12px;
}

.qat-button:hover {
    -fx-background-color: rgba(255,255,255,0.20);
    -fx-text-fill: #FFFFFF;
}

.qat-button:pressed {
    -fx-background-color: rgba(0,0,0,0.15);
}

/* ─── Área de Conteúdo Principal ───────────────────────── */
.content-area {
    -fx-background-color: #F5F5F5;
    -fx-padding: 0;
}

/* ─── Barra de Estado (StatusBar) ──────────────────────── */
.status-bar {
    -fx-background-color: #217346;
    -fx-min-height: 22px;
    -fx-pref-height: 22px;
    -fx-max-height: 22px;
    -fx-padding: 0 8 0 8;
}

.status-bar-label {
    -fx-text-fill: rgba(255,255,255,0.90);
    -fx-font-size: 11px;
}

.status-bar-separator {
    -fx-background-color: rgba(255,255,255,0.25);
    -fx-pref-width: 1;
    -fx-min-height: 12px;
    -fx-pref-height: 12px;
}

/* ─── Painel Lateral / Navigation Drawer ───────────────── */
.nav-panel {
    -fx-background-color: #1E6B3C;
    -fx-pref-width: 220px;
    -fx-min-width: 48px;
}

.nav-item {
    -fx-background-color: transparent;
    -fx-text-fill: rgba(255,255,255,0.80);
    -fx-font-size: 13px;
    -fx-padding: 10 16 10 16;
    -fx-cursor: hand;
    -fx-alignment: center-left;
    -fx-pref-width: infinity;
    -fx-background-radius: 0;
}

.nav-item:hover {
    -fx-background-color: rgba(255,255,255,0.12);
    -fx-text-fill: #FFFFFF;
}

.nav-item:selected,
.nav-item.active {
    -fx-background-color: rgba(255,255,255,0.20);
    -fx-text-fill: #FFFFFF;
    -fx-font-weight: bold;
    -fx-border-color: transparent transparent transparent #FFFFFF;
    -fx-border-width: 0 0 0 3;
}

.nav-section-header {
    -fx-text-fill: rgba(255,255,255,0.50);
    -fx-font-size: 10px;
    -fx-font-weight: bold;
    -fx-padding: 16 16 4 16;
    -fx-text-transform: uppercase;
}

/* ─── Cards / Painéis de Conteúdo ──────────────────────── */
.card {
    -fx-background-color: #FFFFFF;
    -fx-background-radius: 4;
    -fx-border-color: #E0E0E0;
    -fx-border-radius: 4;
    -fx-border-width: 1;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4, 0, 0, 1);
    -fx-padding: 16;
}

.card-title {
    -fx-font-size: 14px;
    -fx-font-weight: bold;
    -fx-text-fill: #212121;
    -fx-padding: 0 0 8 0;
}

.card-subtitle {
    -fx-font-size: 12px;
    -fx-text-fill: #757575;
}

/* ─── Tabelas ───────────────────────────────────────────── */
.table-view {
    -fx-background-color: #FFFFFF;
    -fx-border-color: #E0E0E0;
    -fx-border-width: 1;
    -fx-background-insets: 0;
    -fx-padding: 0;
}

.table-view .column-header-background {
    -fx-background-color: #E8F5E9;
    -fx-border-color: transparent transparent #A5D6A7 transparent;
    -fx-border-width: 0 0 1 0;
}

.table-view .column-header,
.table-view .filler {
    -fx-background-color: transparent;
    -fx-border-color: transparent #D0D0D0 transparent transparent;
    -fx-border-width: 0 1 0 0;
    -fx-size: 32px;
}

.table-view .column-header .label {
    -fx-text-fill: #1E6B3C;
    -fx-font-weight: bold;
    -fx-font-size: 12px;
    -fx-alignment: center-left;
    -fx-padding: 0 8 0 8;
}

.table-row-cell {
    -fx-background-color: #FFFFFF;
    -fx-border-color: transparent transparent #F5F5F5 transparent;
    -fx-border-width: 0 0 1 0;
    -fx-cell-size: 30px;
    -fx-text-fill: #212121;
    -fx-font-size: 12px;
}

.table-row-cell:odd {
    -fx-background-color: #FAFAFA;
}

.table-row-cell:selected {
    -fx-background-color: #E8F5E9;
    -fx-text-fill: #1E6B3C;
}

.table-row-cell:selected:focused {
    -fx-background-color: #C8E6C9;
}

.table-cell {
    -fx-padding: 4 8 4 8;
    -fx-text-fill: inherit;
}

/* ─── Botões ────────────────────────────────────────────── */
.button {
    -fx-background-color: #EEEEEE;
    -fx-text-fill: #212121;
    -fx-background-radius: 3;
    -fx-border-color: #BDBDBD;
    -fx-border-radius: 3;
    -fx-border-width: 1;
    -fx-font-size: 12px;
    -fx-padding: 6 16 6 16;
    -fx-cursor: hand;
}

.button:hover {
    -fx-background-color: #E0E0E0;
}

.button:pressed {
    -fx-background-color: #BDBDBD;
}

.button-primary {
    -fx-background-color: #217346;
    -fx-text-fill: #FFFFFF;
    -fx-border-color: #1B5E3B;
    -fx-font-weight: bold;
}

.button-primary:hover {
    -fx-background-color: #1E6B3C;
}

.button-primary:pressed {
    -fx-background-color: #185C37;
}

.button-secondary {
    -fx-background-color: transparent;
    -fx-text-fill: #217346;
    -fx-border-color: #217346;
}

.button-secondary:hover {
    -fx-background-color: #E8F5E9;
}

.button-danger {
    -fx-background-color: #D32F2F;
    -fx-text-fill: #FFFFFF;
    -fx-border-color: #B71C1C;
}

.button-danger:hover {
    -fx-background-color: #C62828;
}

/* ─── Campos de Texto ───────────────────────────────────── */
.text-field, .text-area, .combo-box, .date-picker .text-field {
    -fx-background-color: #FFFFFF;
    -fx-border-color: #BDBDBD;
    -fx-border-radius: 3;
    -fx-border-width: 1;
    -fx-background-radius: 3;
    -fx-font-size: 12px;
    -fx-text-fill: #212121;
    -fx-prompt-text-fill: #9E9E9E;
    -fx-padding: 5 8 5 8;
}

.text-field:focused, .text-area:focused,
.combo-box:focused .text-field {
    -fx-border-color: #217346;
    -fx-border-width: 2;
    -fx-effect: dropshadow(gaussian, rgba(33,115,70,0.15), 4, 0, 0, 0);
}

.text-field:hover, .text-area:hover {
    -fx-border-color: #757575;
}

/* ─── ComboBox ──────────────────────────────────────────── */
.combo-box {
    -fx-background-color: #FFFFFF;
    -fx-border-color: #BDBDBD;
    -fx-border-radius: 3;
    -fx-background-radius: 3;
}

.combo-box .list-cell {
    -fx-background-color: #FFFFFF;
    -fx-text-fill: #212121;
    -fx-font-size: 12px;
    -fx-padding: 4 8 4 8;
}

.combo-box-popup .list-view {
    -fx-border-color: #217346;
    -fx-border-width: 1;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 3);
}

.combo-box-popup .list-cell:filled:hover {
    -fx-background-color: #E8F5E9;
    -fx-text-fill: #1E6B3C;
}

.combo-box-popup .list-cell:filled:selected {
    -fx-background-color: #217346;
    -fx-text-fill: #FFFFFF;
}

/* ─── Labels ────────────────────────────────────────────── */
.label {
    -fx-text-fill: #212121;
    -fx-font-size: 12px;
}

.label-title {
    -fx-font-size: 20px;
    -fx-font-weight: bold;
    -fx-text-fill: #1E6B3C;
}

.label-subtitle {
    -fx-font-size: 14px;
    -fx-text-fill: #424242;
}

.label-caption {
    -fx-font-size: 11px;
    -fx-text-fill: #757575;
}

.label-required {
    -fx-text-fill: #D32F2F;
    -fx-font-size: 12px;
}

/* ─── Menus ─────────────────────────────────────────────── */
.menu-bar {
    -fx-background-color: #217346;
    -fx-padding: 0;
}

.menu-bar .menu {
    -fx-background-color: transparent;
}

.menu-bar .menu .label {
    -fx-text-fill: rgba(255,255,255,0.90);
    -fx-font-size: 12px;
}

.menu-bar .menu:hover,
.menu-bar .menu:showing {
    -fx-background-color: rgba(255,255,255,0.20);
}

.context-menu {
    -fx-background-color: #FFFFFF;
    -fx-border-color: #E0E0E0;
    -fx-border-width: 1;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 10, 0, 0, 3);
    -fx-background-radius: 3;
    -fx-padding: 4 0 4 0;
}

.menu-item {
    -fx-background-color: transparent;
    -fx-padding: 6 24 6 12;
}

.menu-item .label {
    -fx-text-fill: #212121;
    -fx-font-size: 12px;
}

.menu-item:focused {
    -fx-background-color: #E8F5E9;
}

.menu-item:focused .label {
    -fx-text-fill: #1E6B3C;
}

/* ─── ScrollBar ─────────────────────────────────────────── */
.scroll-bar {
    -fx-background-color: transparent;
    -fx-pref-width: 10px;
    -fx-pref-height: 10px;
}

.scroll-bar .track {
    -fx-background-color: #F5F5F5;
    -fx-border-color: transparent;
    -fx-background-radius: 5;
}

.scroll-bar .thumb {
    -fx-background-color: #BDBDBD;
    -fx-background-radius: 5;
}

.scroll-bar .thumb:hover {
    -fx-background-color: #757575;
}

.scroll-bar .increment-button,
.scroll-bar .decrement-button {
    -fx-background-color: transparent;
    -fx-padding: 0;
    -fx-pref-height: 0;
    -fx-pref-width: 0;
}

/* ─── Tabs (TabPane secundário — painéis internos) ─────── */
.tab-pane {
    -fx-background-color: transparent;
}

.tab-pane .tab-header-area {
    -fx-background-color: #F5F5F5;
    -fx-padding: 0 0 0 4;
}

.tab-pane .tab {
    -fx-background-color: transparent;
    -fx-border-color: transparent;
    -fx-padding: 6 14 6 14;
}

.tab-pane .tab .tab-label {
    -fx-text-fill: #616161;
    -fx-font-size: 12px;
}

.tab-pane .tab:selected {
    -fx-background-color: #FFFFFF;
    -fx-border-color: transparent transparent #217346 transparent;
    -fx-border-width: 0 0 2 0;
}

.tab-pane .tab:selected .tab-label {
    -fx-text-fill: #217346;
    -fx-font-weight: bold;
}

.tab-pane .tab:hover .tab-label {
    -fx-text-fill: #212121;
}

/* ─── CheckBox / RadioButton ────────────────────────────── */
.check-box .box {
    -fx-background-color: #FFFFFF;
    -fx-border-color: #BDBDBD;
    -fx-border-radius: 2;
    -fx-border-width: 1;
}

.check-box:selected .box {
    -fx-background-color: #217346;
    -fx-border-color: #217346;
}

.check-box:selected .mark {
    -fx-background-color: #FFFFFF;
}

.radio-button .radio {
    -fx-background-color: #FFFFFF;
    -fx-border-color: #BDBDBD;
    -fx-border-width: 1;
}

.radio-button:selected .radio {
    -fx-background-color: #FFFFFF;
    -fx-border-color: #217346;
}

.radio-button:selected .dot {
    -fx-background-color: #217346;
}

/* ─── Diálogos ──────────────────────────────────────────── */
.dialog-pane {
    -fx-background-color: #FFFFFF;
    -fx-border-color: #E0E0E0;
}

.dialog-pane .header-panel {
    -fx-background-color: #1E6B3C;
    -fx-padding: 16 24 16 24;
}

.dialog-pane .header-panel .label {
    -fx-text-fill: #FFFFFF;
    -fx-font-size: 14px;
    -fx-font-weight: bold;
}

.dialog-pane .button-bar {
    -fx-background-color: #F5F5F5;
    -fx-border-color: #E0E0E0 transparent transparent transparent;
    -fx-border-width: 1 0 0 0;
    -fx-padding: 12 16 12 16;
}

/* ─── Separadores visuais ───────────────────────────────── */
.separator .line {
    -fx-border-color: #E0E0E0;
    -fx-border-width: 1;
}

/* ─── Tooltips ──────────────────────────────────────────── */
.tooltip {
    -fx-background-color: #424242;
    -fx-text-fill: #FFFFFF;
    -fx-font-size: 11px;
    -fx-background-radius: 3;
    -fx-padding: 4 8 4 8;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 6, 0, 0, 2);
}

/* ─── ProgressBar ───────────────────────────────────────── */
.progress-bar .track {
    -fx-background-color: #E0E0E0;
    -fx-background-radius: 3;
}

.progress-bar .bar {
    -fx-background-color: #217346;
    -fx-background-radius: 3;
    -fx-padding: 4px;
}

/* ─── Spinner ───────────────────────────────────────────── */
.spinner {
    -fx-border-color: #BDBDBD;
    -fx-border-radius: 3;
}

.spinner:focused {
    -fx-border-color: #217346;
}

.spinner .increment-arrow-button,
.spinner .decrement-arrow-button {
    -fx-background-color: #F5F5F5;
}

.spinner .increment-arrow-button:hover,
.spinner .decrement-arrow-button:hover {
    -fx-background-color: #E8F5E9;
}

/* ─── Badge / Chip (classes utilitárias) ────────────────── */
.badge {
    -fx-background-color: #217346;
    -fx-text-fill: #FFFFFF;
    -fx-background-radius: 10;
    -fx-font-size: 10px;
    -fx-font-weight: bold;
    -fx-padding: 2 6 2 6;
    -fx-min-width: 18px;
    -fx-alignment: center;
}

.badge-warning {
    -fx-background-color: #F9A825;
    -fx-text-fill: #212121;
}

.badge-danger {
    -fx-background-color: #D32F2F;
    -fx-text-fill: #FFFFFF;
}

.badge-info {
    -fx-background-color: #0277BD;
    -fx-text-fill: #FFFFFF;
}

/* ─── Alertas inline ────────────────────────────────────── */
.alert-success {
    -fx-background-color: #E8F5E9;
    -fx-border-color: #A5D6A7;
    -fx-border-width: 1;
    -fx-border-radius: 3;
    -fx-background-radius: 3;
    -fx-padding: 10 14 10 14;
}

.alert-warning {
    -fx-background-color: #FFFDE7;
    -fx-border-color: #FFF176;
    -fx-border-width: 1;
    -fx-border-radius: 3;
    -fx-background-radius: 3;
    -fx-padding: 10 14 10 14;
}

.alert-error {
    -fx-background-color: #FFEBEE;
    -fx-border-color: #FFCDD2;
    -fx-border-width: 1;
    -fx-border-radius: 3;
    -fx-background-radius: 3;
    -fx-padding: 10 14 10 14;
}
```

---

## 2. `ribbon.css` — CSS do Ribbon Actualizado (Tema Verde)

Substitui o ficheiro existente em: `src/main/resources/ao/allon/kubata/admin/ui/ribbon/ribbon.css`

```css
/* ═══════════════════════════════════════════════════
   RIBBON — Tema Verde Kubata (Estilo Excel 365 Verde)
   ═══════════════════════════════════════════════════ */

/* Raiz do Ribbon */
.ribbon-bar {
    -fx-background-color: #217346;
    -fx-border-color: transparent transparent #1B5E3B transparent;
    -fx-border-width: 0 0 1 0;
}

/* Barra de separadores */
.ribbon-tab-bar {
    -fx-background-color: #217346;
    -fx-min-height: 32px;
    -fx-pref-height: 32px;
}

/* Botões de separador */
.ribbon-tab-button {
    -fx-background-color: transparent;
    -fx-text-fill: rgba(255,255,255,0.85);
    -fx-font-size: 12px;
    -fx-font-family: "Segoe UI", "Calibri", "Arial", sans-serif;
    -fx-padding: 6 14 6 14;
    -fx-background-radius: 0;
    -fx-border-radius: 0;
    -fx-cursor: hand;
}

.ribbon-tab-button:hover {
    -fx-background-color: rgba(255,255,255,0.15);
    -fx-text-fill: #FFFFFF;
}

.ribbon-tab-button:selected {
    -fx-background-color: #FFFFFF;
    -fx-text-fill: #1E6B3C;
    -fx-font-weight: bold;
    -fx-border-color: transparent;
    -fx-border-width: 0;
    -fx-background-radius: 3 3 0 0;
}

/* Área de conteúdo do separador */
.ribbon-tab-content-area {
    -fx-background-color: #FFFFFF;
    -fx-min-height: 90px;
    -fx-pref-height: 90px;
    -fx-max-height: 90px;
    -fx-border-color: transparent transparent #C8E6C9 transparent;
    -fx-border-width: 0 0 1 0;
}

/* Conteúdo do tab (HBox de grupos) */
.ribbon-tab-content {
    -fx-background-color: #FFFFFF;
    -fx-min-height: 90px;
}

/* Grupo */
.ribbon-group {
    -fx-background-color: transparent;
    -fx-min-height: 88px;
    -fx-pref-height: 88px;
    -fx-max-height: 88px;
    -fx-border-color: transparent #C8E6C9 transparent transparent;
    -fx-border-width: 0 1 0 0;
    -fx-padding: 0;
}

/* Título do grupo */
.ribbon-group-title {
    -fx-font-size: 10px;
    -fx-text-fill: #757575;
    -fx-font-family: "Segoe UI", "Calibri", "Arial", sans-serif;
    -fx-alignment: center;
    -fx-max-width: infinity;
    -fx-padding: 1 4 2 4;
}

/* Separador horizontal entre conteúdo e título do grupo */
.ribbon-group-separator {
    -fx-background-color: #C8E6C9;
    -fx-pref-height: 1;
    -fx-min-height: 1;
    -fx-max-height: 1;
}

/* Separador vertical entre colunas de botões */
.ribbon-column-separator {
    -fx-background-color: transparent;
    -fx-pref-width: 1;
    -fx-min-width: 1;
}

/* Coluna de botões */
.ribbon-button-column {
    -fx-alignment: top-left;
    -fx-spacing: 1;
}

/* Botão do Ribbon (base) */
.ribbon-button {
    -fx-background-color: transparent;
    -fx-background-radius: 3;
    -fx-border-color: transparent;
    -fx-border-radius: 3;
    -fx-border-width: 1;
    -fx-cursor: hand;
    -fx-padding: 0;
}

.ribbon-button:hover {
    -fx-background-color: #E8F5E9;
    -fx-border-color: #A5D6A7;
}

.ribbon-button:pressed {
    -fx-background-color: #C8E6C9;
    -fx-border-color: #81C784;
}

/* Botão LARGE */
.ribbon-button-large {
    -fx-min-width: 48px;
    -fx-pref-width: 56px;
    -fx-min-height: 62px;
    -fx-pref-height: 66px;
    -fx-padding: 2 4 2 4;
}

/* Botão SMALL */
.ribbon-button-small {
    -fx-min-width: 80px;
    -fx-pref-width: 100px;
    -fx-min-height: 20px;
    -fx-pref-height: 22px;
    -fx-padding: 1 4 1 4;
}

/* Texto dos botões */
.ribbon-button-text {
    -fx-font-size: 11px;
    -fx-fill: #212121;
    -fx-font-family: "Segoe UI", "Calibri", "Arial", sans-serif;
}

/* Ícone LARGE (32×32) */
.ribbon-icon-large {
    -fx-pref-width: 32px;
    -fx-pref-height: 32px;
    -fx-min-width: 32px;
    -fx-min-height: 32px;
    -fx-icon-color: #217346;
}

/* Ícone SMALL (16×16) */
.ribbon-icon-small {
    -fx-pref-width: 16px;
    -fx-pref-height: 16px;
    -fx-min-width: 16px;
    -fx-min-height: 16px;
    -fx-icon-color: #217346;
}

/* Botão de overflow "»" */
.ribbon-overflow-button {
    -fx-background-color: #F1F8F3;
    -fx-text-fill: #217346;
    -fx-font-size: 14px;
    -fx-min-width: 24px;
    -fx-pref-width: 24px;
    -fx-min-height: 88px;
    -fx-pref-height: 88px;
    -fx-background-radius: 0;
    -fx-border-color: #C8E6C9;
    -fx-border-width: 0 0 0 1;
    -fx-cursor: hand;
    -fx-padding: 0;
}

.ribbon-overflow-button:hover {
    -fx-background-color: #C8E6C9;
}

/* Popup de overflow */
.ribbon-overflow-popup {
    -fx-background-color: #FFFFFF;
    -fx-border-color: #C8E6C9;
    -fx-border-width: 1;
    -fx-background-radius: 4;
    -fx-padding: 8;
    -fx-effect: dropshadow(gaussian, rgba(33,115,70,0.18), 10, 0, 0, 3);
}
```

---

## 3. `CustomTitleBar.java` — Barra de Título com Campo de Pesquisa

Cria em: `src/main/java/ao/allon/kubata/admin/ui/component/CustomTitleBar.java`

```java
package ao.allon.kubata.admin.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;

import java.util.function.Consumer;

/**
 * Barra de título personalizada ao estilo Excel 365 Verde.
 *
 * Estrutura visual:
 * ┌──────────────────────────────────────────────────────────────────┐
 * │ [🟢Ícone] [Nome App]   [🔍 Pesquisar...]    [User] [─][□][✕] │
 * └──────────────────────────────────────────────────────────────────┘
 *
 * Uso:
 *   CustomTitleBar titleBar = new CustomTitleBar(stage, "Kubata Faturação");
 *   titleBar.setOnSearch(query -> System.out.println("Pesquisa: " + query));
 *   titleBar.setUserName("João Silva");
 *   root.setTop(titleBar);
 *
 *   // Lembrar de desactivar a decoração nativa:
 *   stage.initStyle(StageStyle.UNDECORATED);
 */
public class CustomTitleBar extends HBox {

    private final Stage stage;
    private final TextField searchField;
    private final Label userLabel;
    private final Label userAvatar;

    // Para arrastar a janela
    private double dragOffsetX;
    private double dragOffsetY;

    private Consumer<String> onSearchCallback;

    public CustomTitleBar(Stage stage, String appTitle) {
        this.stage = stage;

        getStyleClass().add("custom-title-bar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(0);
        setPadding(new Insets(0, 0, 0, 0));

        // ── Ícone da aplicação ──────────────────────────────
        FontIcon appIcon = new FontIcon(MaterialDesignF.FILE_DOCUMENT_OUTLINE);
        appIcon.setIconSize(20);
        appIcon.setIconColor(javafx.scene.paint.Color.WHITE);
        appIcon.setPadding(new Insets(0, 6, 0, 12));
        getStyleClass().add("title-bar-app-icon");

        // ── Nome da aplicação ───────────────────────────────
        Label nameLabel = new Label(appTitle);
        nameLabel.getStyleClass().add("title-bar-app-name");

        // ── Espaço flexível esquerdo ────────────────────────
        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.SOMETIMES);
        leftSpacer.setMinWidth(24);

        // ── Campo de Pesquisa ───────────────────────────────
        searchField = new TextField();
        searchField.setPromptText("Pesquisar na aplicação...");
        searchField.getStyleClass().add("title-bar-search-box");

        // Ícone de pesquisa (sobreposição visual)
        FontIcon searchIcon = new FontIcon(MaterialDesignM.MAGNIFY);
        searchIcon.setIconSize(14);
        searchIcon.setIconColor(javafx.scene.paint.Color.web("rgba(255,255,255,0.70)"));

        // Container do campo de pesquisa com ícone
        StackPane searchContainer = new StackPane();
        searchContainer.setAlignment(Pos.CENTER_LEFT);
        searchContainer.getChildren().addAll(searchField);

        // Posicionamento do ícone dentro do campo
        searchIcon.setTranslateX(10);
        searchContainer.getChildren().add(searchIcon);
        StackPane.setAlignment(searchIcon, Pos.CENTER_LEFT);

        // Acção ao pressionar Enter
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                triggerSearch(searchField.getText().trim());
            }
        });

        // ── Espaço flexível direito ─────────────────────────
        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.SOMETIMES);
        rightSpacer.setMinWidth(24);

        // ── Info do utilizador ──────────────────────────────
        userAvatar = new Label("U");
        userAvatar.getStyleClass().add("title-bar-user-avatar");
        userAvatar.setTooltip(new Tooltip("Perfil do utilizador"));

        userLabel = new Label("");
        userLabel.getStyleClass().add("title-bar-user-label");
        userLabel.setVisible(false);
        userLabel.setManaged(false);

        HBox userBox = new HBox(6, userLabel, userAvatar);
        userBox.setAlignment(Pos.CENTER);
        userBox.setPadding(new Insets(0, 8, 0, 8));

        // ── Botões de controlo da janela ────────────────────
        Button minimizeBtn = createWindowButton("─", "Minimizar", false);
        Button maximizeBtn = createWindowButton("□", "Maximizar / Restaurar", false);
        Button closeBtn    = createWindowButton("✕", "Fechar", true);

        minimizeBtn.setOnAction(e -> stage.setIconified(true));
        maximizeBtn.setOnAction(e -> {
            stage.setMaximized(!stage.isMaximized());
        });
        closeBtn.setOnAction(e -> stage.close());

        // ── Montagem ────────────────────────────────────────
        getChildren().addAll(
            appIcon,
            nameLabel,
            leftSpacer,
            searchContainer,
            rightSpacer,
            userBox,
            minimizeBtn,
            maximizeBtn,
            closeBtn
        );

        // ── Arrastar a janela ───────────────────────────────
        setupDragging();

        // ── Duplo-clique para maximizar ─────────────────────
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !isOnControlButton(e.getX())) {
                stage.setMaximized(!stage.isMaximized());
            }
        });
    }

    // ── API pública ─────────────────────────────────────────

    /**
     * Define o callback executado quando o utilizador pesquisa.
     * Chamado ao pressionar Enter no campo ou ao clicar no ícone.
     */
    public void setOnSearch(Consumer<String> callback) {
        this.onSearchCallback = callback;
    }

    /**
     * Define o nome do utilizador autenticado.
     * As iniciais são calculadas automaticamente para o avatar.
     */
    public void setUserName(String name) {
        userLabel.setText(name);
        userLabel.setVisible(true);
        userLabel.setManaged(true);

        // Iniciais: até 2 letras maiúsculas
        String[] parts = name.trim().split("\\s+");
        String initials = parts.length >= 2
            ? String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)
            : name.length() > 0 ? String.valueOf(name.charAt(0)) : "U";
        userAvatar.setText(initials.toUpperCase());
    }

    /**
     * Limpa o campo de pesquisa.
     */
    public void clearSearch() {
        searchField.clear();
    }

    /**
     * Define texto de sugestão no campo de pesquisa.
     */
    public void setSearchPrompt(String prompt) {
        searchField.setPromptText(prompt);
    }

    // ── Privado ─────────────────────────────────────────────

    private void triggerSearch(String query) {
        if (onSearchCallback != null && !query.isEmpty()) {
            onSearchCallback.accept(query);
        }
    }

    private Button createWindowButton(String symbol, String tooltip, boolean isClose) {
        Button btn = new Button(symbol);
        btn.getStyleClass().add(isClose ? "title-bar-close-btn" : "title-bar-control-btn");
        btn.setTooltip(new Tooltip(tooltip));
        btn.setFocusTraversable(false);
        return btn;
    }

    private void setupDragging() {
        setOnMousePressed(e -> {
            dragOffsetX = e.getSceneX();
            dragOffsetY = e.getSceneY();
        });

        setOnMouseDragged(e -> {
            if (!stage.isMaximized()) {
                stage.setX(e.getScreenX() - dragOffsetX);
                stage.setY(e.getScreenY() - dragOffsetY);
            }
        });
    }

    /** Verifica se o clique foi nos botões de controlo (evita maximizar acidentalmente). */
    private boolean isOnControlButton(double x) {
        double w = getWidth();
        return x > w - 138; // aproximadamente 3 botões × 46px
    }
}
```

---

## 4. `ThemeManager.java` — Actualizado

Substitui o ficheiro em: `src/main/java/ao/allon/kubata/admin/ui/util/ThemeManager.java`

```java
package ao.allon.kubata.admin.ui.util;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public final class ThemeManager {

    // ── Paleta Kubata Verde ──────────────────────────────
    public static final String KUBATA_GREEN_DARK   = "#1E6B3C";
    public static final String KUBATA_GREEN        = "#217346";
    public static final String KUBATA_GREEN_MEDIUM = "#2E7D32";
    public static final String KUBATA_GREEN_LIGHT  = "#4CAF50";
    public static final String KUBATA_GREEN_PALE   = "#E8F5E9";
    public static final String KUBATA_GREEN_BORDER = "#A5D6A7";

    private static final String ADMIN_CSS_PATH =
        "/ao/allon/kubata/admin/ui/styles/admin.css";
    private static final String RIBBON_CSS_PATH =
        "/ao/allon/kubata/admin/ui/ribbon/ribbon.css";

    private ThemeManager() {}

    /**
     * Aplica o tema completo (admin.css + ribbon.css) à Scene.
     * Chamar uma vez após criar a Scene principal.
     */
    public static void applyTheme(Scene scene) {
        loadStylesheet(scene, ADMIN_CSS_PATH);
        loadStylesheet(scene, RIBBON_CSS_PATH);
    }

    /**
     * Configura o Stage para usar barra de título personalizada.
     * IMPORTANTE: Chamar ANTES de stage.show().
     * Após isto, adicionar CustomTitleBar ao topo do layout.
     */
    public static void setupCustomTitleBar(Stage stage) {
        stage.initStyle(StageStyle.UNDECORATED);
    }

    private static void loadStylesheet(Scene scene, String path) {
        var resource = ThemeManager.class.getResource(path);
        if (resource != null) {
            scene.getStylesheets().add(resource.toExternalForm());
        } else {
            System.err.println("[ThemeManager] CSS não encontrado: " + path);
        }
    }
}
```

---

## 5. Integração na Janela Principal

### Exemplo de `MainWindow.java` (ou equivalente)

```java
// No controller/view principal que recebe o StageReadyEvent:

@Component
public class MainWindowController implements ApplicationListener<StageReadyEvent> {

    @Autowired
    private SessionManager sessionManager;

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        Stage stage = event.getStage();

        // 1. Configurar barra de título personalizada (ANTES de show)
        ThemeManager.setupCustomTitleBar(stage);

        // 2. Construir layout principal
        BorderPane root = new BorderPane();
        root.getStyleClass().add("main-window");

        // 3. Barra de título personalizada
        CustomTitleBar titleBar = new CustomTitleBar(stage, "Kubata Faturação");
        titleBar.setOnSearch(query -> handleSearch(query));
        root.setTop(titleBar);

        // 4. Ribbon abaixo da barra de título
        RibbonBar ribbon = buildRibbon();
        VBox topArea = new VBox(0, titleBar, ribbon);
        root.setTop(topArea);

        // 5. Área de conteúdo principal
        root.setCenter(buildContentArea());

        // 6. Barra de estado
        root.setBottom(buildStatusBar());

        // 7. Criar Scene e aplicar tema
        Scene scene = new Scene(root, 1280, 800);
        ThemeManager.applyTheme(scene);

        stage.setScene(scene);
        stage.setTitle("Kubata Faturação");
        stage.setMinWidth(800);
        stage.setMinHeight(500);
        stage.show();
    }

    // Ao receber evento de login bem-sucedido:
    @EventListener
    public void onLoginSuccess(LoginSuccessEvent event) {
        Platform.runLater(() -> {
            titleBar.setUserName(event.getUser().getNome());
        });
    }

    private void handleSearch(String query) {
        // Implementar lógica de pesquisa global
        System.out.println("Pesquisar: " + query);
    }
}
```

---

## 6. Paleta de Cores — Referência Rápida

| Token                   | Hex       | Uso Principal                      |
|-------------------------|-----------|------------------------------------|
| `KUBATA_GREEN_DARK`     | `#1E6B3C` | Barra de título, cabeçalhos        |
| `KUBATA_GREEN`          | `#217346` | Ribbon, botões primários, status   |
| `KUBATA_GREEN_MEDIUM`   | `#2E7D32` | Hover, estados activos             |
| `KUBATA_GREEN_LIGHT`    | `#4CAF50` | Focus rings, accents               |
| `KUBATA_GREEN_PALE`     | `#E8F5E9` | Fundos de cards, linhas par        |
| `KUBATA_GREEN_BORDER`   | `#A5D6A7` | Bordas suaves, separadores         |

---

## 7. Checklist de Implementação

- [ ] Criar pasta `src/main/resources/ao/allon/kubata/admin/ui/styles/`
- [ ] Copiar `admin.css` para essa pasta
- [ ] Substituir `ribbon.css` com a versão actualizada
- [ ] Criar `CustomTitleBar.java` em `ui/component/`
- [ ] Actualizar `ThemeManager.java`
- [ ] No ponto de entrada da janela principal, chamar `ThemeManager.setupCustomTitleBar(stage)` **antes** de `stage.show()`
- [ ] Chamar `ThemeManager.applyTheme(scene)` após criar a `Scene`
- [ ] Adicionar `CustomTitleBar` ao topo do `BorderPane` principal
- [ ] Verificar dependências Ikonli no `pom.xml` (MaterialDesign2 pack)

### Dependência Ikonli necessária (pom.xml)
```xml
<dependency>
    <groupId>org.kordamp.ikonli</groupId>
    <artifactId>ikonli-materialdesign2-pack</artifactId>
    <version>${ikonli.version}</version>
</dependency>
```
