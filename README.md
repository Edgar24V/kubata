# Kubata - Sistema de Faturação Modular para Angola

Sistema de faturação desenvolvido com Java 21, JavaFX e Spring Boot.

## Estrutura do Projeto

O projeto é um sistema multi-módulo Maven:

- **kubata-parent**: Configurações globais e gestão de dependências.
- **kubata-core**: Entidades base e configurações compartilhadas.
- **kubata-faturacao**: Módulo principal de faturação com interface JavaFX.

## Tecnologias

- Java 21
- JavaFX 21
- Spring Boot 3.2+
- Hibernate 6.4+
- Flyway (Migrations)
- SQLite (Dev) / PostgreSQL (Prod)
- Lombok

## Como Executar

1. Certifique-se de ter o JDK 21 instalado (ou configure para uma versão compatível no `pom.xml`).
2. Compile o projeto:
   ```bash
   mvn clean install
   ```
3. Execute o módulo de faturação:
   ```bash
   cd faturacao
   mvn spring-boot:run
   ```

## Funcionalidades

- Gestão de Clientes (CRUD)
- Emissão de Faturas (Criação, Edição, Cancelamento)
- Cálculo automático de impostos (IVA)
- Dashboard com estatísticas
