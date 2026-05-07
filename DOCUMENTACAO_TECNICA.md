# Documentação Técnica - KUBATA Faturação

## Visão Geral
O **KUBATA Faturação** é um sistema de gestão empresarial (ERP) desenvolvido especificamente para o mercado angolano. Ele combina a robustez do Spring Boot no backend com a interatividade do JavaFX (enriquecido pela biblioteca Atlantafx) no frontend desktop.

## Arquitetura do Sistema

### Tecnologias Principais
- **Backend/Core**: Java 21, Spring Boot 3.x (Dependency Injection, JPA, Transaction Management).
- **Frontend/UI**: JavaFX 21, Atlantafx (Tema moderno e componentes estilizados).
- **Persistência**: Hibernate/JPA, suporte a SQLite (desenvolvimento), PostgreSQL (produção).
- **Relatórios**: Geração de CSV e XML (SAFT-AO), JasperReports.
- **Build**: Maven.

### Estrutura de Pacotes
- `ao.allon.kubata.faturacao.domain`: Entidades JPA (Produto, Fatura, Cliente, etc.).
- `ao.allon.kubata.faturacao.repository`: Interfaces Spring Data JPA.
- `ao.allon.kubata.faturacao.service`: Lógica de negócio (@Service).
- `ao.allon.kubata.faturacao.controller`: Controladores da UI (MVC pattern).
- `ao.allon.kubata.faturacao.view`: Classes de visualização JavaFX.
- `ao.allon.kubata.faturacao.ui.util`: Utilitários de interface (IconUtils, AlertUtils).
- `ao.allon.kubata.faturacao.ui.modal`: Serviço de gerenciamento de modais (ModalService).

## Módulos Principais

### 1. Gestão de Produtos e Estoque (`ProdutoService`, `EstoqueService`)
- **Cadastro**: Suporte a código de barras, unidade de medida, categorização.
- **Movimentação**: Entrada/Saída manual, Transferência entre armazéns.
- **Rastreabilidade**: Controle por lote e validade.
- **Conformidade**: IVA padrão de 14% (configurável), Preços em Kwanza (AOA).

### 2. Gestão de Fornecedores (`FornecedorService`)
- Cadastro completo com NIF, Endereço e Contactos.
- Avaliação de desempenho e Termos de Pagamento.

### 3. Faturação e Vendas (`FaturaService`, `PdvController`)
- Emissão de Faturas e Recibos.
- Frente de Caixa (PDV) para vendas rápidas.
- Integração com estoque (baixa automática).

### 4. Relatórios e Fiscalidade
- **SAFT-AO**: Exportação de dados no formato padrão de auditoria fiscal de Angola (`SaftAoExportService`).
- **CSV**: Exportação de listagens para Excel.

### 5. Backup e Manutenção
- Backup automático da base de dados configurado via `@Scheduled` (`DatabaseBackupService`).
- Execução diária às 02:00 AM (configurável).

### 6. Guias de Remessa e Transporte
- Emissão, listagem e impressão de Guias de Remessa (GR) e Guias de Transporte (GT).
- Numeração sequencial por Série específica com controlo de hash.
- Campos logísticos: origem, destino, tipo de transporte, matrícula e motorista.
- Conversão direta em Fatura.

### 7. Gestão de Encomendas
- Criação e acompanhamento de Encomendas (EC) como documentos de trabalho.
- Conversão em Fatura para processamento fiscal.
- Monitoramento de status e previsão de entrega.

### 8. Gestão de Devoluções
- Workflow completo de devolução: Solicitação -> Análise -> Aprovação -> Processamento.
- Geração automática de Nota de Crédito ao concluir o processo.
- Atualização de estoque integrada.
- Histórico de auditoria com parecer técnico e responsáveis.

## Configuração e Instalação

### Requisitos
- JDK 21.
- Maven 3.8+.
- Banco de dados SQLite (padrão) ou PostgreSQL.

### Execução
```bash
mvn spring-boot:run
```

### Perfis de Execução
O sistema utiliza perfis do Spring (`dev`, `prod`). Configurações podem ser ajustadas em `application.properties`.

## Notas de Desenvolvimento
- A interface utiliza o `ModalService` (builder pattern) para diálogos consistentes e desacoplados.
- O tema visual é baseado no Primer Dark (GitHub Dark), customizado via `styles.css` e Atlantafx.
- **Importante**: Não utilizar estilos arredondados (`border-radius: 0`) em Modais e Notificações conforme diretriz de design.
