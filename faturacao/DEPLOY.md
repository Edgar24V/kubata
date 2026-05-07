# Guia de Deploy e Migração de Ambientes

Este documento descreve o processo de deploy e gestão de ambientes para a aplicação **Kubata Faturação**, utilizando **SQLite** para Desenvolvimento/Testes e **PostgreSQL** para Produção.

## Estrutura de Migração

A aplicação utiliza o **Flyway** para versionamento de banco de dados, com scripts segregados por tipo de banco:

- **SQLite (Dev/Test)**: `src/main/resources/db/migration/sqlite`
- **PostgreSQL (Prod)**: `src/main/resources/db/migration/postgresql`

## Perfis de Configuração

A aplicação seleciona automaticamente as migrações e configurações corretas baseada no perfil ativo:

| Perfil | Banco de Dados | Arquivo de Configuração | Uso Recomendado |
| :--- | :--- | :--- | :--- |
| `sqlite` | SQLite (Arquivo) | `application-sqlite.properties` | Desenvolvimento Local |
| `test` | SQLite (Memória) | `application-test.properties` | Testes Automatizados |
| `prod` | PostgreSQL | `application-prod.properties` | Produção / Homologação |

O perfil padrão é `sqlite`.

## Deploy em Produção (PostgreSQL)

### Pré-requisitos
- Servidor PostgreSQL 13+
- Java 17+
- Variáveis de ambiente configuradas

### Configuração de Variáveis de Ambiente
Configure as seguintes variáveis no servidor ou container:

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:postgresql://<HOST>:<PORT>/<DB_NAME>
export DB_USERNAME=<SEU_USUARIO>
export DB_PASSWORD=<SUA_SENHA>
export DB_SCHEMA=public
```

### Execução
Para iniciar a aplicação em modo de produção:

```bash
java -jar kubata-faturacao.jar
```
Ou passando as propriedades via linha de comando:

```bash
java -Dspring.profiles.active=prod \
     -Dspring.datasource.url=jdbc:postgresql://localhost:5432/kubata_db \
     -Dspring.datasource.username=postgres \
     -Dspring.datasource.password=secret \
     -jar kubata-faturacao.jar
```

## Desenvolvimento e Testes (SQLite)

Para rodar localmente com SQLite (banco criado em `kubata.db`):
```bash
mvn spring-boot:run
# ou
mvn spring-boot:run -Dspring-boot.run.profiles=sqlite
```

Para rodar os testes:
```bash
mvn test
```

## Validando Migrações

### Adicionando Nova Migração
1. Crie o script SQL na pasta `src/main/resources/db/migration/sqlite` com sintaxe compatível com SQLite.
2. Crie o script equivalente na pasta `src/main/resources/db/migration/postgresql` com sintaxe PostgreSQL (`BIGSERIAL`, `TIMESTAMP`, etc.).
3. Mantenha o versionamento (`Vxx__Descricao.sql`) sincronizado entre as pastas.

### Testes de Integridade
O pipeline de testes valida automaticamente a integridade dos scripts:
- `FlywayMigrationTest`: Valida migrações SQLite.
- `PostgresMigrationTest`: Valida migrações PostgreSQL (usando H2 em modo de compatibilidade).
