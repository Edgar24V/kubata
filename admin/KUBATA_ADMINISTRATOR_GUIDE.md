# 🏛️ KUBATA ADMINISTRATOR — GUIA COMPLETO
### Transformar o Módulo Administrator ao Nível do Primavera ERP

> **Versão:** 1.0.0 | **Stack:** JavaFX 21 · Spring Boot 3.x · AtlantaFX · Maven Multi-Module · MySQL/SQLite · Flyway  
> **Destino:** Angolan Enterprise Standard | **Locale:** `pt-AO` | **Moeda:** AOA | **Timezone:** `Africa/Luanda`

---

## 📋 ÍNDICE GERAL

1. [Visão Geral e Arquitectura](#1-visão-geral-e-arquitectura)
2. [Estrutura Maven do Módulo Administrator](#2-estrutura-maven-do-módulo-administrator)
3. [Entidades JPA — Modelo de Dados Completo](#3-entidades-jpa--modelo-de-dados-completo)
4. [Repositórios e Serviços](#4-repositórios-e-serviços)
5. [Formulários e Views JavaFX](#5-formulários-e-views-javafx)
6. [Páginas e Navegação (TabPane + Ribbon)](#6-páginas-e-navegação-tabpane--ribbon)
7. [Sistema de Permissões e Perfis](#7-sistema-de-permissões-e-perfis)
8. [Configuração Fiscal Angolana (AGT)](#8-configuração-fiscal-angolana-agt)
9. [Séries de Documentos SAF-T AO](#9-séries-de-documentos-saf-t-ao)
10. [Gestão de Módulos (Plugin System)](#10-gestão-de-módulos-plugin-system)
11. [Auditoria e Logs do Sistema](#11-auditoria-e-logs-do-sistema)
12. [Backup e Manutenção da Base de Dados](#12-backup-e-manutenção-da-base-de-dados)
13. [CSS — Tema Verde Excel 365 para o Administrator](#13-css--tema-verde-excel-365-para-o-administrator)
14. [Migrações Flyway — Scripts SQL Completos](#14-migrações-flyway--scripts-sql-completos)
15. [Guia de Instalação Step-by-Step](#15-guia-de-instalação-step-by-step)
16. [Exercícios Práticos](#16-exercícios-práticos)
17. [Checklist de Homologação](#17-checklist-de-homologação) (inclui árvore do ribbon e mapa de recursos)

---

## 1. Visão Geral e Arquitectura

### 1.1 O que é o Módulo Administrator no Primavera ERP?

O **Primavera Administrator** é o módulo central de controlo e configuração de todo o ecossistema ERP. Ele gere:

| Área | Função |
|---|---|
| **Empresa** | Dados fiscais, logótipo, contactos, configuração AGT |
| **Utilizadores** | Criação, edição, bloqueio, reset de password |
| **Perfis de Acesso** | Grupos de permissões por módulo e operação |
| **Séries de Documentos** | FT, FR, NC, ND, GD, GT, CC, ORC, etc. |
| **Parâmetros do Sistema** | Configurações globais, moeda, IVA padrão |
| **Módulos Instalados** | Activar/desactivar módulos do sistema |
| **Auditoria** | Log de todas as operações críticas |
| **Exercícios Fiscais** | Anos fiscais e períodos contabilísticos |
| **Moedas e Câmbios** | AOA como base, câmbios para USD, EUR |
| **Backup e Restauro** | Gestão da base de dados |

### 1.2 Arquitectura Alvo

```
kubata-root/
├── kubata-administrator/          ← Módulo que vamos construir
│   ├── src/main/java/
│   │   └── ao/kubata/admin/
│   │       ├── config/            ← Spring @Configuration
│   │       ├── domain/            ← Entidades JPA
│   │       ├── repository/        ← Spring Data JPA
│   │       ├── service/           ← Lógica de negócio
│   │       ├── controller/        ← JavaFX Controllers
│   │       └── view/              ← FXML Views
│   └── src/main/resources/
│       ├── fxml/admin/            ← Ficheiros FXML
│       ├── css/admin/             ← CSS do módulo
│       └── db/migration/admin/    ← Flyway scripts
│
├── kubata-core/                   ← Módulo core (já existente)
├── kubata-billing/                ← Módulo de faturação
├── kubata-hr/                     ← Módulo de RH
└── kubata-app/                    ← Módulo launcher/main
```

---

## 2. Estrutura Maven do Módulo Administrator

### 2.1 `pom.xml` do Módulo Administrator

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>ao.kubata</groupId>
        <artifactId>kubata-root</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>kubata-administrator</artifactId>
    <name>Kubata Administrator</name>
    <description>Módulo de Administração do Sistema Kubata</description>

    <dependencies>
        <!-- Core do Kubata -->
        <dependency>
            <groupId>ao.kubata</groupId>
            <artifactId>kubata-core</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- JavaFX -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-fxml</artifactId>
        </dependency>

        <!-- AtlantaFX -->
        <dependency>
            <groupId>io.github.mkpaz</groupId>
            <artifactId>atlantafx-base</artifactId>
        </dependency>

        <!-- ControlsFX -->
        <dependency>
            <groupId>org.controlsfx</groupId>
            <artifactId>controlsfx</artifactId>
        </dependency>

        <!-- Flyway -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-mysql</artifactId>
        </dependency>

        <!-- BCrypt para passwords -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-crypto</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- ikonli para ícones -->
        <dependency>
            <groupId>org.kordamp.ikonli</groupId>
            <artifactId>ikonli-javafx</artifactId>
        </dependency>
        <dependency>
            <groupId>org.kordamp.ikonli</groupId>
            <artifactId>ikonli-material2-pack</artifactId>
        </dependency>
        <dependency>
            <groupId>org.kordamp.ikonli</groupId>
            <artifactId>ikonli-materialdesign2-pack</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 2.2 Adicionar ao `pom.xml` raiz

```xml
<!-- No pom.xml raiz, em <modules> -->
<modules>
    <module>kubata-core</module>
    <module>kubata-administrator</module>   <!-- ADICIONAR -->
    <module>kubata-billing</module>
    <module>kubata-hr</module>
    <module>kubata-app</module>
</modules>
```

---

## 3. Entidades JPA — Modelo de Dados Completo

### 3.1 Entidade `Empresa` (Company)

```java
// ao/kubata/admin/domain/Empresa.java
package ao.kubata.admin.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "adm_empresa")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identificação Fiscal ──────────────────────────────────────
    @Column(name = "nif", nullable = false, unique = true, length = 14)
    @NotBlank(message = "NIF é obrigatório")
    @Pattern(regexp = "^\\d{9,14}$", message = "NIF inválido")
    private String nif;

    @Column(name = "denominacao_social", nullable = false, length = 200)
    @NotBlank(message = "Denominação Social é obrigatória")
    private String denominacaoSocial;

    @Column(name = "nome_comercial", length = 150)
    private String nomeComercial;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contribuinte", nullable = false)
    private TipoContribuinte tipoContribuinte;

    @Enumerated(EnumType.STRING)
    @Column(name = "regime_iva", nullable = false)
    private RegimeIVA regimeIVA;

    // ── Localização ───────────────────────────────────────────────
    @Column(name = "endereco", length = 300)
    private String endereco;

    @Column(name = "municipio", length = 100)
    private String municipio;

    @Column(name = "provincia", length = 100)
    private String provincia;

    @Column(name = "pais", length = 3)
    @Builder.Default
    private String pais = "AO";

    @Column(name = "caixa_postal", length = 20)
    private String caixaPostal;

    // ── Contactos ─────────────────────────────────────────────────
    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "telemovel", length = 20)
    private String telemovel;

    @Column(name = "fax", length = 20)
    private String fax;

    @Column(name = "email")
    @Email(message = "Email inválido")
    private String email;

    @Column(name = "website", length = 100)
    private String website;

    // ── Dados Bancários ───────────────────────────────────────────
    @Column(name = "iban", length = 34)
    private String iban;

    @Column(name = "banco", length = 100)
    private String banco;

    @Column(name = "conta_bancaria", length = 30)
    private String contaBancaria;

    // ── Dados AGT / SAF-T AO ──────────────────────────────────────
    @Column(name = "codigo_cae", length = 10)
    private String codigoCAE;

    @Column(name = "descricao_actividade", length = 200)
    private String descricaoActividade;

    @Column(name = "data_constituicao")
    private LocalDate dataConstituicao;

    @Column(name = "conservatoria", length = 100)
    private String conservatoria;

    @Column(name = "matricula_comercial", length = 50)
    private String matriculaComercial;

    @Column(name = "capital_social")
    private Double capitalSocial;

    // ── Certificação AGT ─────────────────────────────────────────
    @Column(name = "numero_certificado_agt", length = 50)
    private String numeroCertificadoAGT;

    @Column(name = "versao_certificado_agt", length = 20)
    private String versaoCertificadoAGT;

    @Column(name = "data_certificado_agt")
    private LocalDate dataCertificadoAGT;

    @Column(name = "hash_certificado_agt", length = 64)
    private String hashCertificadoAGT;

    // ── Configuração de Software ──────────────────────────────────
    @Column(name = "moeda_base", length = 3)
    @Builder.Default
    private String moedaBase = "AOA";

    @Column(name = "casas_decimais_valor")
    @Builder.Default
    private Integer casasDecimaisValor = 2;

    @Column(name = "casas_decimais_quantidade")
    @Builder.Default
    private Integer casasDecimaisQuantidade = 3;

    @Column(name = "exercicio_actual")
    private Integer exercicioActual;

    // ── Logótipo ──────────────────────────────────────────────────
    @Lob
    @Column(name = "logotipo", columnDefinition = "LONGBLOB")
    private byte[] logotipo;

    @Column(name = "logotipo_mime_type", length = 50)
    private String logotipoMimeType;

    // ── Configuração de Impressão ─────────────────────────────────
    @Column(name = "rodape_documento", length = 500)
    private String rodapeDocumento;

    @Column(name = "mensagem_fatura", length = 300)
    private String mensagemFatura;

    // ── Auditoria ─────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    // ── Enums Internos ────────────────────────────────────────────
    public enum TipoContribuinte {
        PESSOA_SINGULAR("Pessoa Singular"),
        PESSOA_COLECTIVA("Pessoa Colectiva"),
        NAO_RESIDENTE("Não Residente");

        private final String descricao;
        TipoContribuinte(String descricao) { this.descricao = descricao; }
        public String getDescricao() { return descricao; }
        @Override public String toString() { return descricao; }
    }

    public enum RegimeIVA {
        GERAL("Regime Geral - 14%"),
        SIMPLIFICADO("Regime Simplificado"),
        ISENTO("Isento de IVA"),
        ESPECIAL("Regime Especial");

        private final String descricao;
        RegimeIVA(String descricao) { this.descricao = descricao; }
        public String getDescricao() { return descricao; }
        @Override public String toString() { return descricao; }
    }
}
```

### 3.2 Entidade `Utilizador` (User)

```java
// ao/kubata/admin/domain/Utilizador.java
package ao.kubata.admin.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "adm_utilizador",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Utilizador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Credenciais ───────────────────────────────────────────────
    @Column(name = "username", nullable = false, unique = true, length = 50)
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @Column(name = "password_hash", nullable = false)
    @NotBlank
    private String passwordHash;

    @Column(name = "password_provisoria")
    @Builder.Default
    private Boolean passwordProvisoria = true;

    @Column(name = "data_expiracao_password")
    private LocalDate dataExpiracaoPassword;

    // ── Dados Pessoais ────────────────────────────────────────────
    @Column(name = "nome_completo", nullable = false, length = 150)
    @NotBlank
    private String nomeCompleto;

    @Column(name = "email", unique = true)
    @Email
    private String email;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "departamento", length = 100)
    private String departamento;

    @Column(name = "cargo", length = 100)
    private String cargo;

    @Lob
    @Column(name = "avatar", columnDefinition = "MEDIUMBLOB")
    private byte[] avatar;

    // ── Estado da Conta ───────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Builder.Default
    private EstadoUtilizador estado = EstadoUtilizador.ACTIVO;

    @Column(name = "tentativas_login_falhas")
    @Builder.Default
    private Integer tentativasLoginFalhas = 0;

    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;

    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;

    @Column(name = "ultimo_ip_login", length = 45)
    private String ultimoIpLogin;

    @Column(name = "superadmin")
    @Builder.Default
    private Boolean superAdmin = false;

    // ── Configurações do Utilizador ───────────────────────────────
    @Column(name = "idioma", length = 5)
    @Builder.Default
    private String idioma = "pt-AO";

    @Column(name = "tema", length = 30)
    @Builder.Default
    private String tema = "VERDE_ADMIN";

    @Column(name = "linha_por_pagina")
    @Builder.Default
    private Integer linhasPorPagina = 50;

    // ── Perfis de Acesso ──────────────────────────────────────────
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "adm_utilizador_perfil",
        joinColumns = @JoinColumn(name = "utilizador_id"),
        inverseJoinColumns = @JoinColumn(name = "perfil_id")
    )
    @Builder.Default
    private Set<PerfilAcesso> perfis = new HashSet<>();

    // ── Empresa associada ─────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    // ── Auditoria ─────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "criado_por", length = 50)
    private String criadoPor;

    // ── Enum ──────────────────────────────────────────────────────
    public enum EstadoUtilizador {
        ACTIVO("Activo"),
        INACTIVO("Inactivo"),
        BLOQUEADO("Bloqueado"),
        PENDENTE("Pendente Activação"),
        EXPIRADO("Password Expirada");

        private final String descricao;
        EstadoUtilizador(String descricao) { this.descricao = descricao; }
        public String getDescricao() { return descricao; }
        @Override public String toString() { return descricao; }
    }
}
```

### 3.3 Entidade `PerfilAcesso` (Role/Permission Profile)

```java
// ao/kubata/admin/domain/PerfilAcesso.java
package ao.kubata.admin.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "adm_perfil_acesso")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;  // Ex: ADMIN, FATURADOR, CONSULTOR

    @Column(name = "descricao", nullable = false, length = 100)
    private String descricao;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @Column(name = "sistema")
    @Builder.Default
    private Boolean sistema = false;  // Perfis do sistema não podem ser apagados

    @Column(name = "activo")
    @Builder.Default
    private Boolean activo = true;

    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PermissaoPerfil> permissoes = new HashSet<>();

    @ManyToMany(mappedBy = "perfis")
    @Builder.Default
    private Set<Utilizador> utilizadores = new HashSet<>();
}
```

### 3.4 Entidade `PermissaoPerfil` (Fine-grained Permission)

```java
// ao/kubata/admin/domain/PermissaoPerfil.java
package ao.kubata.admin.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "adm_permissao_perfil",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"perfil_id", "modulo", "recurso", "operacao"}
    ))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissaoPerfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_id", nullable = false)
    private PerfilAcesso perfil;

    @Column(name = "modulo", nullable = false, length = 50)
    private String modulo;       // Ex: FATURACAO, CLIENTES, STOCK

    @Column(name = "recurso", nullable = false, length = 50)
    private String recurso;      // Ex: FATURAS, CLIENTES, PRODUTOS

    @Enumerated(EnumType.STRING)
    @Column(name = "operacao", nullable = false)
    private Operacao operacao;   // VER, CRIAR, EDITAR, APAGAR, IMPRIMIR, EXPORTAR

    @Column(name = "permitido", nullable = false)
    @Builder.Default
    private Boolean permitido = false;

    public enum Operacao {
        VER, CRIAR, EDITAR, APAGAR, IMPRIMIR, EXPORTAR, APROVAR, ANULAR
    }
}
```

### 3.5 Entidade `SerieDocumento` (Document Series)

```java
// ao/kubata/admin/domain/SerieDocumento.java
package ao.kubata.admin.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "adm_serie_documento",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"tipo_documento", "serie", "empresa_id"}
    ))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerieDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    // ── Identificação da Série ────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false)
    private TipoDocumentoSAFT tipoDocumento;

    @Column(name = "serie", nullable = false, length = 10)
    @NotBlank
    private String serie;  // Ex: A, B, 2024, SEDE

    @Column(name = "descricao", length = 200)
    private String descricao;  // Ex: "Série Principal 2024"

    // ── Numeração ─────────────────────────────────────────────────
    @Column(name = "ultimo_numero", nullable = false)
    @Builder.Default
    private Long ultimoNumero = 0L;

    @Column(name = "numero_inicial", nullable = false)
    @Builder.Default
    private Long numeroInicial = 1L;

    @Column(name = "prefixo", length = 10)
    private String prefixo;  // Ex: FT, FR, NC

    @Column(name = "formato_numero", length = 30)
    @Builder.Default
    private String formatoNumero = "{PREFIXO} {SERIE}/{NUMERO}";
    // Resultado: FT A/1, FT A/2, ...

    // ── Validade ──────────────────────────────────────────────────
    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "exercicio")
    private Integer exercicio;  // Ano fiscal: 2024, 2025

    // ── Estado ────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Builder.Default
    private EstadoSerie estado = EstadoSerie.ACTIVA;

    @Column(name = "predefinida")
    @Builder.Default
    private Boolean predefinida = false;

    // ── Comunicação AGT ───────────────────────────────────────────
    @Column(name = "registada_agt")
    @Builder.Default
    private Boolean registadaAGT = false;

    @Column(name = "data_registo_agt")
    private LocalDateTime dataRegistoAGT;

    @Column(name = "codigo_validacao_agt", length = 50)
    private String codigoValidacaoAGT;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    // ── Método utilitário ─────────────────────────────────────────
    public String gerarProximoNumero() {
        ultimoNumero++;
        String formato = formatoNumero
            .replace("{PREFIXO}", tipoDocumento.getPrefixo())
            .replace("{SERIE}", serie)
            .replace("{NUMERO}", String.format("%06d", ultimoNumero))
            .replace("{ANO}", String.valueOf(exercicio));
        return formato;
    }

    // ── Enums ─────────────────────────────────────────────────────
    public enum TipoDocumentoSAFT {
        FT("FT", "Factura", "Vendas"),
        FR("FR", "Factura Recibo", "Vendas"),
        NC("NC", "Nota de Crédito", "Vendas"),
        ND("ND", "Nota de Débito", "Vendas"),
        GD("GD", "Guia de Devolução", "Stock"),
        GT("GT", "Guia de Transporte", "Stock"),
        GR("GR", "Guia de Remessa", "Stock"),
        CC("CC", "Consulta de Cotação", "Vendas"),
        ORC("ORC", "Orçamento", "Vendas"),
        ECF("ECF", "Extracto de Conta Factura", "Financeiro"),
        RG("RG", "Recibo", "Financeiro"),
        PG("PG", "Pagamento", "Financeiro"),
        DC("DC", "Documento de Compra", "Compras"),
        VD("VD", "Venda a Dinheiro", "Vendas");

        private final String prefixo;
        private final String nome;
        private final String area;

        TipoDocumentoSAFT(String prefixo, String nome, String area) {
            this.prefixo = prefixo;
            this.nome = nome;
            this.area = area;
        }

        public String getPrefixo() { return prefixo; }
        public String getNome() { return nome; }
        public String getArea() { return area; }
        @Override public String toString() { return prefixo + " - " + nome; }
    }

    public enum EstadoSerie {
        ACTIVA("Activa"),
        INACTIVA("Inactiva"),
        ENCERRADA("Encerrada"),
        PENDENTE_AGT("Pendente Registo AGT");

        private final String descricao;
        EstadoSerie(String d) { this.descricao = d; }
        public String getDescricao() { return descricao; }
        @Override public String toString() { return descricao; }
    }
}
```

### 3.6 Entidade `ExercicioFiscal` (Fiscal Year)

```java
// ao/kubata/admin/domain/ExercicioFiscal.java
package ao.kubata.admin.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "adm_exercicio_fiscal",
    uniqueConstraints = @UniqueConstraint(columnNames = {"empresa_id", "ano"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExercicioFiscal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "ano", nullable = false)
    private Integer ano;  // 2024, 2025

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Builder.Default
    private EstadoExercicio estado = EstadoExercicio.ABERTO;

    @Column(name = "encerrado_em")
    private java.time.LocalDateTime encerradoEm;

    @Column(name = "encerrado_por", length = 50)
    private String encerradoPor;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    public enum EstadoExercicio {
        FUTURO("Futuro"),
        ABERTO("Aberto"),
        ENCERRAMENTO("Em Encerramento"),
        FECHADO("Fechado");

        private final String descricao;
        EstadoExercicio(String d) { this.descricao = d; }
        @Override public String toString() { return descricao; }
    }
}
```

### 3.7 Entidade `Moeda` (Currency)

```java
// ao/kubata/admin/domain/Moeda.java
package ao.kubata.admin.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "adm_moeda")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Moeda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_iso", nullable = false, unique = true, length = 3)
    private String codigoISO;  // AOA, USD, EUR, GBP

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "simbolo", length = 10)
    private String simbolo;  // Kz, $, €

    @Column(name = "taxa_cambio", precision = 18, scale = 6)
    private BigDecimal taxaCambio;  // Relativo ao AOA

    @Column(name = "data_taxa_cambio")
    private LocalDate dataTaxaCambio;

    @Column(name = "moeda_base")
    @Builder.Default
    private Boolean moedaBase = false;  // Apenas AOA = true

    @Column(name = "casas_decimais")
    @Builder.Default
    private Integer casasDecimais = 2;

    @Column(name = "activa")
    @Builder.Default
    private Boolean activa = true;
}
```

### 3.8 Entidade `ModuloSistema` (System Module)

```java
// ao/kubata/admin/domain/ModuloSistema.java
package ao.kubata.admin.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "adm_modulo_sistema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuloSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;  // BILLING, HR, STOCK, ADMIN, POS

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @Column(name = "versao", length = 20)
    private String versao;  // 1.0.0

    @Column(name = "versao_minima_core", length = 20)
    private String versaoMinimаCore;

    @Column(name = "icone_classe", length = 100)
    private String iconeClasse;  // MDI2B_CASH_REGISTER

    @Column(name = "cor_hex", length = 7)
    private String corHex;  // #4CAF50

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Builder.Default
    private EstadoModulo estado = EstadoModulo.DISPONIVEL;

    @Column(name = "obrigatorio")
    @Builder.Default
    private Boolean obrigatorio = false;  // ADMIN é sempre obrigatório

    @Column(name = "instalado_em")
    private LocalDateTime instaladoEm;

    @Column(name = "desactivado_em")
    private LocalDateTime desactivadoEm;

    @Column(name = "licenca_chave", length = 200)
    private String licencaChave;

    @Column(name = "licenca_validade")
    private LocalDateTime licencaValidade;

    @Column(name = "ordem_menu")
    @Builder.Default
    private Integer ordemMenu = 99;

    public enum EstadoModulo {
        DISPONIVEL("Disponível"),
        ACTIVO("Activo"),
        INACTIVO("Inactivo"),
        ERRO("Erro"),
        ACTUALIZACAO_PENDENTE("Actualização Pendente");

        private final String descricao;
        EstadoModulo(String d) { this.descricao = d; }
        @Override public String toString() { return descricao; }
    }
}
```

### 3.9 Entidade `LogAuditoria` (Audit Log)

```java
// ao/kubata/admin/domain/LogAuditoria.java
package ao.kubata.admin.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "adm_log_auditoria",
    indexes = {
        @Index(name = "idx_log_utilizador", columnList = "utilizador_id"),
        @Index(name = "idx_log_entidade", columnList = "entidade, entidade_id"),
        @Index(name = "idx_log_data", columnList = "criado_em"),
        @Index(name = "idx_log_operacao", columnList = "operacao")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Quem fez ──────────────────────────────────────────────────
    @Column(name = "utilizador_id")
    private Long utilizadorId;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // ── O quê ─────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "operacao", nullable = false)
    private TipoOperacao operacao;

    @Column(name = "modulo", length = 50)
    private String modulo;

    @Column(name = "entidade", length = 100)
    private String entidade;  // "Utilizador", "Factura", "Cliente"

    @Column(name = "entidade_id", length = 50)
    private String entidadeId;

    @Column(name = "descricao", length = 1000)
    private String descricao;  // Descrição legível da operação

    @Column(name = "dados_anteriores", columnDefinition = "TEXT")
    private String dadosAnteriores;  // JSON com estado anterior

    @Column(name = "dados_novos", columnDefinition = "TEXT")
    private String dadosNovos;  // JSON com novo estado

    // ── Quando ────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "duracao_ms")
    private Long duracaoMs;

    // ── Resultado ─────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "resultado")
    @Builder.Default
    private ResultadoOperacao resultado = ResultadoOperacao.SUCESSO;

    @Column(name = "mensagem_erro", length = 2000)
    private String mensagemErro;

    public enum TipoOperacao {
        LOGIN, LOGOUT, LOGIN_FALHOU,
        CRIAR, EDITAR, APAGAR, CONSULTAR,
        IMPRIMIR, EXPORTAR, IMPORTAR,
        APROVAR, ANULAR, ENCERRAR,
        BACKUP, RESTAURO,
        CONFIGURACAO, INSTALACAO_MODULO
    }

    public enum ResultadoOperacao {
        SUCESSO, FALHA, PARCIAL
    }
}
```

### 3.10 Entidade `ParametroSistema` (System Parameter)

```java
// ao/kubata/admin/domain/ParametroSistema.java
package ao.kubata.admin.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "adm_parametro_sistema",
    uniqueConstraints = @UniqueConstraint(columnNames = {"chave", "empresa_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametroSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;  // null = parâmetro global

    @Column(name = "chave", nullable = false, length = 100)
    private String chave;  // Ex: IVA_PADRAO, MOEDA_BASE, DIAS_VENCIMENTO

    @Column(name = "valor", length = 1000)
    private String valor;

    @Column(name = "tipo_valor", length = 20)
    @Builder.Default
    private String tipoValor = "STRING";  // STRING, INTEGER, DECIMAL, BOOLEAN, DATE

    @Column(name = "descricao", length = 500)
    private String descricao;

    @Column(name = "editavel")
    @Builder.Default
    private Boolean editavel = true;

    @Column(name = "grupo", length = 50)
    private String grupo;  // FISCAL, FINANCEIRO, SISTEMA, INTERFACE

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "atualizado_por", length = 50)
    private String atualizadoPor;
}
```

---

## 4. Repositórios e Serviços

### 4.1 Repositórios Spring Data JPA

```java
// ao/kubata/admin/repository/EmpresaRepository.java
package ao.kubata.admin.repository;

import ao.kubata.admin.domain.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    Optional<Empresa> findByNif(String nif);
    Optional<Empresa> findFirstByAtivoTrue();
}
```

```java
// ao/kubata/admin/repository/UtilizadorRepository.java
package ao.kubata.admin.repository;

import ao.kubata.admin.domain.Utilizador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UtilizadorRepository extends JpaRepository<Utilizador, Long> {
    Optional<Utilizador> findByUsername(String username);
    Optional<Utilizador> findByEmail(String email);
    List<Utilizador> findByEstado(Utilizador.EstadoUtilizador estado);
    
    @Query("SELECT u FROM Utilizador u WHERE " +
           "LOWER(u.nomeCompleto) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<Utilizador> pesquisar(String q);
    
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

```java
// ao/kubata/admin/repository/SerieDocumentoRepository.java
package ao.kubata.admin.repository;

import ao.kubata.admin.domain.SerieDocumento;
import ao.kubata.admin.domain.SerieDocumento.TipoDocumentoSAFT;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SerieDocumentoRepository extends JpaRepository<SerieDocumento, Long> {
    List<SerieDocumento> findByTipoDocumento(TipoDocumentoSAFT tipo);
    List<SerieDocumento> findByTipoDocumentoAndEstado(
        TipoDocumentoSAFT tipo, SerieDocumento.EstadoSerie estado);
    Optional<SerieDocumento> findByTipoDocumentoAndPredefinidaTrue(TipoDocumentoSAFT tipo);
}
```

```java
// ao/kubata/admin/repository/LogAuditoriaRepository.java
package ao.kubata.admin.repository;

import ao.kubata.admin.domain.LogAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    Page<LogAuditoria> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    Page<LogAuditoria> findByCriadoEmBetween(
        LocalDateTime inicio, LocalDateTime fim, Pageable pageable);

    @Query("SELECT l FROM LogAuditoria l WHERE " +
           "(:username IS NULL OR LOWER(l.username) LIKE LOWER(CONCAT('%',:username,'%'))) AND " +
           "(:operacao IS NULL OR l.operacao = :operacao) AND " +
           "(:modulo IS NULL OR l.modulo = :modulo) AND " +
           "(:inicio IS NULL OR l.criadoEm >= :inicio) AND " +
           "(:fim IS NULL OR l.criadoEm <= :fim) " +
           "ORDER BY l.criadoEm DESC")
    Page<LogAuditoria> pesquisarLogs(
        String username, LogAuditoria.TipoOperacao operacao,
        String modulo, LocalDateTime inicio, LocalDateTime fim,
        Pageable pageable);
}
```

### 4.2 Serviço `UtilizadorService`

```java
// ao/kubata/admin/service/UtilizadorService.java
package ao.kubata.admin.service;

import ao.kubata.admin.domain.*;
import ao.kubata.admin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UtilizadorService {

    private final UtilizadorRepository utilizadorRepository;
    private final LogAuditoriaRepository logAuditoriaRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final int MAX_TENTATIVAS = 5;
    private static final int MINUTOS_BLOQUEIO = 30;

    public Utilizador criarUtilizador(Utilizador utilizador, String passwordPlain) {
        // Validar unicidade
        if (utilizadorRepository.existsByUsername(utilizador.getUsername())) {
            throw new IllegalArgumentException(
                "Username '" + utilizador.getUsername() + "' já existe");
        }
        if (utilizador.getEmail() != null &&
            utilizadorRepository.existsByEmail(utilizador.getEmail())) {
            throw new IllegalArgumentException("Email já está em uso");
        }

        // Encriptar password
        utilizador.setPasswordHash(passwordEncoder.encode(passwordPlain));
        utilizador.setPasswordProvisoria(true);
        utilizador.setDataExpiracaoPassword(LocalDate.now().plusDays(30));
        utilizador.setEstado(Utilizador.EstadoUtilizador.ACTIVO);

        Utilizador saved = utilizadorRepository.save(utilizador);
        
        // Auditar
        registarLog(LogAuditoria.TipoOperacao.CRIAR, "Utilizador", 
            saved.getId().toString(), "Utilizador criado: " + saved.getUsername());

        log.info("Utilizador criado: {}", saved.getUsername());
        return saved;
    }

    public Optional<Utilizador> autenticar(String username, String passwordPlain) {
        Optional<Utilizador> optUtilizador = utilizadorRepository.findByUsername(username);
        
        if (optUtilizador.isEmpty()) {
            registarLoginFalhado(username, "Utilizador não encontrado");
            return Optional.empty();
        }

        Utilizador utilizador = optUtilizador.get();

        // Verificar bloqueio
        if (utilizador.getEstado() == Utilizador.EstadoUtilizador.BLOQUEADO) {
            if (utilizador.getBloqueadoAte() != null &&
                LocalDateTime.now().isBefore(utilizador.getBloqueadoAte())) {
                throw new IllegalStateException(
                    "Conta bloqueada até " + utilizador.getBloqueadoAte());
            } else {
                // Desbloquear automaticamente
                utilizador.setEstado(Utilizador.EstadoUtilizador.ACTIVO);
                utilizador.setTentativasLoginFalhas(0);
            }
        }

        // Verificar estado
        if (utilizador.getEstado() != Utilizador.EstadoUtilizador.ACTIVO &&
            utilizador.getEstado() != Utilizador.EstadoUtilizador.EXPIRADO) {
            throw new IllegalStateException("Conta inactiva");
        }

        // Verificar password
        if (!passwordEncoder.matches(passwordPlain, utilizador.getPasswordHash())) {
            incrementarFalhas(utilizador);
            registarLoginFalhado(username, "Password incorrecta");
            return Optional.empty();
        }

        // Login com sucesso
        utilizador.setTentativasLoginFalhas(0);
        utilizador.setUltimoLogin(LocalDateTime.now());
        utilizadorRepository.save(utilizador);

        registarLog(LogAuditoria.TipoOperacao.LOGIN, "Utilizador",
            utilizador.getId().toString(), "Login bem sucedido: " + username);

        return Optional.of(utilizador);
    }

    public void alterarPassword(Long utilizadorId, String passwordActual,
                                 String novaPassword, String confirmacao) {
        if (!novaPassword.equals(confirmacao)) {
            throw new IllegalArgumentException("As passwords não coincidem");
        }
        if (novaPassword.length() < 8) {
            throw new IllegalArgumentException("A password deve ter pelo menos 8 caracteres");
        }

        Utilizador utilizador = utilizadorRepository.findById(utilizadorId)
            .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado"));

        if (!passwordEncoder.matches(passwordActual, utilizador.getPasswordHash())) {
            throw new IllegalArgumentException("Password actual incorrecta");
        }

        utilizador.setPasswordHash(passwordEncoder.encode(novaPassword));
        utilizador.setPasswordProvisoria(false);
        utilizador.setDataExpiracaoPassword(LocalDate.now().plusDays(365));
        utilizadorRepository.save(utilizador);

        registarLog(LogAuditoria.TipoOperacao.EDITAR, "Utilizador",
            utilizadorId.toString(), "Password alterada");
    }

    public void bloquearUtilizador(Long id, String motivo) {
        Utilizador u = utilizadorRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado"));
        u.setEstado(Utilizador.EstadoUtilizador.BLOQUEADO);
        utilizadorRepository.save(u);
        registarLog(LogAuditoria.TipoOperacao.EDITAR, "Utilizador",
            id.toString(), "Utilizador bloqueado: " + motivo);
    }

    public void desbloquearUtilizador(Long id) {
        Utilizador u = utilizadorRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado"));
        u.setEstado(Utilizador.EstadoUtilizador.ACTIVO);
        u.setTentativasLoginFalhas(0);
        u.setBloqueadoAte(null);
        utilizadorRepository.save(u);
        registarLog(LogAuditoria.TipoOperacao.EDITAR, "Utilizador",
            id.toString(), "Utilizador desbloqueado");
    }

    public List<Utilizador> listarTodos() {
        return utilizadorRepository.findAll();
    }

    public List<Utilizador> pesquisar(String query) {
        return utilizadorRepository.pesquisar(query);
    }

    // ── Métodos Privados ──────────────────────────────────────────

    private void incrementarFalhas(Utilizador utilizador) {
        int falhas = utilizador.getTentativasLoginFalhas() + 1;
        utilizador.setTentativasLoginFalhas(falhas);
        if (falhas >= MAX_TENTATIVAS) {
            utilizador.setEstado(Utilizador.EstadoUtilizador.BLOQUEADO);
            utilizador.setBloqueadoAte(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEIO));
            log.warn("Utilizador {} bloqueado por {} tentativas falhadas",
                utilizador.getUsername(), falhas);
        }
        utilizadorRepository.save(utilizador);
    }

    private void registarLoginFalhado(String username, String motivo) {
        registarLog(LogAuditoria.TipoOperacao.LOGIN_FALHOU, "Utilizador", null,
            "Tentativa de login falhada para '" + username + "': " + motivo);
    }

    private void registarLog(LogAuditoria.TipoOperacao op, String entidade,
                              String entidadeId, String descricao) {
        LogAuditoria log = LogAuditoria.builder()
            .operacao(op)
            .entidade(entidade)
            .entidadeId(entidadeId)
            .descricao(descricao)
            .modulo("ADMINISTRATOR")
            .resultado(LogAuditoria.ResultadoOperacao.SUCESSO)
            .build();
        logAuditoriaRepository.save(log);
    }
}
```

### 4.3 Serviço `SerieDocumentoService`

```java
// ao/kubata/admin/service/SerieDocumentoService.java
package ao.kubata.admin.service;

import ao.kubata.admin.domain.*;
import ao.kubata.admin.repository.SerieDocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SerieDocumentoService {

    private final SerieDocumentoRepository serieRepository;

    /**
     * Obtém o próximo número de documento de forma ATÓMICA e THREAD-SAFE.
     * Usa @Transactional para garantir que dois documentos nunca têm
     * o mesmo número.
     */
    public synchronized String obterProximoNumero(
            SerieDocumento.TipoDocumentoSAFT tipo) {
        
        SerieDocumento serie = serieRepository
            .findByTipoDocumentoAndPredefinidaTrue(tipo)
            .orElseThrow(() -> new IllegalStateException(
                "Nenhuma série predefinida configurada para " + tipo.getNome()));

        if (serie.getEstado() != SerieDocumento.EstadoSerie.ACTIVA) {
            throw new IllegalStateException(
                "A série '" + serie.getSerie() + "' está " + serie.getEstado());
        }

        String numero = serie.gerarProximoNumero();
        serieRepository.save(serie);
        return numero;
    }

    public SerieDocumento criarSerie(SerieDocumento serie) {
        // Garantir que só existe uma série predefinida por tipo
        if (Boolean.TRUE.equals(serie.getPredefinida())) {
            serieRepository.findByTipoDocumentoAndPredefinidaTrue(serie.getTipoDocumento())
                .ifPresent(existente -> {
                    existente.setPredefinida(false);
                    serieRepository.save(existente);
                });
        }
        return serieRepository.save(serie);
    }

    public void inicializarSeriesPadrao(Empresa empresa, int exercicio) {
        for (SerieDocumento.TipoDocumentoSAFT tipo : 
             SerieDocumento.TipoDocumentoSAFT.values()) {
            
            SerieDocumento serie = SerieDocumento.builder()
                .empresa(empresa)
                .tipoDocumento(tipo)
                .serie("A")
                .descricao(tipo.getNome() + " - Série Principal " + exercicio)
                .exercicio(exercicio)
                .predefinida(true)
                .estado(SerieDocumento.EstadoSerie.ACTIVA)
                .formatoNumero("{PREFIXO} {SERIE}/{NUMERO}")
                .build();
            
            serieRepository.save(serie);
        }
    }

    public List<SerieDocumento> listarTodas() {
        return serieRepository.findAll();
    }
}
```

---

## 5. Formulários e Views JavaFX

### 5.1 Controller Principal do Administrator

```java
// ao/kubata/admin/controller/AdminMainController.java
package ao.kubata.admin.controller;

import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.*;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class AdminMainController implements Initializable {

    @FXML private TabPane mainTabPane;
    @FXML private Label lblTitulo;
    @FXML private Label lblEmpresa;
    @FXML private Label lblUtilizador;
    @FXML private Label lblVersao;
    @FXML private ProgressBar progressBar;
    @FXML private Label lblStatus;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarTabs();
        carregarInformacoesEmpresa();
        progressBar.setVisible(false);
    }

    private void configurarTabs() {
        // As tabs são adicionadas via FXML ou programaticamente
        mainTabPane.getStyleClass().add("admin-tab-pane");
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    }

    private void carregarInformacoesEmpresa() {
        // Carregar dados da sessão actual
    }

    @FXML
    public void abrirEmpresa() {
        seleccionarTab("tab-empresa");
    }

    @FXML
    public void abrirUtilizadores() {
        seleccionarTab("tab-utilizadores");
    }

    private void seleccionarTab(String id) {
        mainTabPane.getTabs().stream()
            .filter(t -> id.equals(t.getId()))
            .findFirst()
            .ifPresent(mainTabPane.getSelectionModel()::select);
    }
}
```

### 5.2 FXML — Tela Principal do Administrator

```xml
<!-- src/main/resources/fxml/admin/AdminMain.fxml -->
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.image.*?>
<?import org.kordamp.ikonli.javafx.*?>
<?import atlantafx.base.controls.*?>

<BorderPane xmlns="http://javafx.com/javafx/21"
            xmlns:fx="http://javafx.com/fxml/1"
            fx:controller="ao.kubata.admin.controller.AdminMainController"
            styleClass="admin-root"
            stylesheets="@../../css/admin/admin-theme.css">

    <!-- ═══ TOPO / TOOLBAR ══════════════════════════════════════════ -->
    <top>
        <VBox>
            <!-- Barra de título -->
            <HBox styleClass="admin-title-bar" alignment="CENTER_LEFT">
                <ImageView fitWidth="32" fitHeight="32"
                           url="@../../images/kubata-logo.png"/>
                <Label text="KUBATA" styleClass="admin-app-name"/>
                <Label text="Administrator" styleClass="admin-module-name"/>
                <Region HBox.hgrow="ALWAYS"/>
                <Label fx:id="lblEmpresa" styleClass="admin-empresa-nome"/>
                <Separator orientation="VERTICAL" styleClass="admin-sep"/>
                <Label fx:id="lblUtilizador" styleClass="admin-user-label"/>
                <Separator orientation="VERTICAL" styleClass="admin-sep"/>
                <Label fx:id="lblVersao" styleClass="admin-version-label"/>
            </HBox>

            <!-- Ribbon / Toolbar de acções -->
            <ToolBar styleClass="admin-ribbon">
                <Button onAction="#abrirEmpresa" styleClass="admin-ribbon-btn">
                    <graphic>
                        <VBox alignment="CENTER" spacing="2">
                            <FontIcon iconLiteral="mdi2b-bank-outline" iconSize="24"
                                      styleClass="admin-icon-green"/>
                            <Label text="Empresa" styleClass="admin-ribbon-label"/>
                        </VBox>
                    </graphic>
                </Button>
                <Separator orientation="VERTICAL"/>
                <Button onAction="#abrirUtilizadores" styleClass="admin-ribbon-btn">
                    <graphic>
                        <VBox alignment="CENTER" spacing="2">
                            <FontIcon iconLiteral="mdi2a-account-group" iconSize="24"
                                      styleClass="admin-icon-green"/>
                            <Label text="Utilizadores" styleClass="admin-ribbon-label"/>
                        </VBox>
                    </graphic>
                </Button>
                <Button styleClass="admin-ribbon-btn">
                    <graphic>
                        <VBox alignment="CENTER" spacing="2">
                            <FontIcon iconLiteral="mdi2s-shield-account" iconSize="24"
                                      styleClass="admin-icon-green"/>
                            <Label text="Perfis" styleClass="admin-ribbon-label"/>
                        </VBox>
                    </graphic>
                </Button>
                <Separator orientation="VERTICAL"/>
                <Button styleClass="admin-ribbon-btn">
                    <graphic>
                        <VBox alignment="CENTER" spacing="2">
                            <FontIcon iconLiteral="mdi2f-file-document-multiple" iconSize="24"
                                      styleClass="admin-icon-green"/>
                            <Label text="Séries" styleClass="admin-ribbon-label"/>
                        </VBox>
                    </graphic>
                </Button>
                <Button styleClass="admin-ribbon-btn">
                    <graphic>
                        <VBox alignment="CENTER" spacing="2">
                            <FontIcon iconLiteral="mdi2c-calendar-range" iconSize="24"
                                      styleClass="admin-icon-green"/>
                            <Label text="Exercícios" styleClass="admin-ribbon-label"/>
                        </VBox>
                    </graphic>
                </Button>
                <Separator orientation="VERTICAL"/>
                <Button styleClass="admin-ribbon-btn">
                    <graphic>
                        <VBox alignment="CENTER" spacing="2">
                            <FontIcon iconLiteral="mdi2p-puzzle" iconSize="24"
                                      styleClass="admin-icon-green"/>
                            <Label text="Módulos" styleClass="admin-ribbon-label"/>
                        </VBox>
                    </graphic>
                </Button>
                <Button styleClass="admin-ribbon-btn">
                    <graphic>
                        <VBox alignment="CENTER" spacing="2">
                            <FontIcon iconLiteral="mdi2c-clipboard-list" iconSize="24"
                                      styleClass="admin-icon-green"/>
                            <Label text="Auditoria" styleClass="admin-ribbon-label"/>
                        </VBox>
                    </graphic>
                </Button>
                <Separator orientation="VERTICAL"/>
                <Button styleClass="admin-ribbon-btn">
                    <graphic>
                        <VBox alignment="CENTER" spacing="2">
                            <FontIcon iconLiteral="mdi2d-database-export" iconSize="24"
                                      styleClass="admin-icon-green"/>
                            <Label text="Backup" styleClass="admin-ribbon-label"/>
                        </VBox>
                    </graphic>
                </Button>
                <Button styleClass="admin-ribbon-btn danger">
                    <graphic>
                        <VBox alignment="CENTER" spacing="2">
                            <FontIcon iconLiteral="mdi2c-cog" iconSize="24"
                                      styleClass="admin-icon-red"/>
                            <Label text="Parâmetros" styleClass="admin-ribbon-label"/>
                        </VBox>
                    </graphic>
                </Button>
            </ToolBar>
        </VBox>
    </top>

    <!-- ═══ CENTRO / CONTEÚDO PRINCIPAL ════════════════════════════ -->
    <center>
        <TabPane fx:id="mainTabPane" styleClass="admin-tab-pane"
                 tabClosingPolicy="UNAVAILABLE">

            <!-- Tab Dashboard -->
            <Tab id="tab-dashboard" text="Dashboard">
                <graphic>
                    <FontIcon iconLiteral="mdi2v-view-dashboard" iconSize="14"
                              styleClass="admin-tab-icon"/>
                </graphic>
                <!-- Conteúdo carregado dinamicamente -->
            </Tab>

            <!-- Tab Empresa -->
            <Tab id="tab-empresa" text="Empresa">
                <graphic>
                    <FontIcon iconLiteral="mdi2b-bank-outline" iconSize="14"
                              styleClass="admin-tab-icon"/>
                </graphic>
            </Tab>

            <!-- Tab Utilizadores -->
            <Tab id="tab-utilizadores" text="Utilizadores">
                <graphic>
                    <FontIcon iconLiteral="mdi2a-account-group" iconSize="14"
                              styleClass="admin-tab-icon"/>
                </graphic>
            </Tab>
        </TabPane>
    </center>

    <!-- ═══ RODAPÉ / STATUS BAR ═════════════════════════════════════ -->
    <bottom>
        <HBox styleClass="admin-status-bar" alignment="CENTER_LEFT" spacing="10">
            <ProgressBar fx:id="progressBar" prefWidth="150"/>
            <Label fx:id="lblStatus" styleClass="admin-status-text"
                   text="Pronto"/>
            <Region HBox.hgrow="ALWAYS"/>
            <Label styleClass="admin-status-hint"
                   text="Kubata Faturação v1.0 | © 2025 Kubata Software, Luanda"/>
        </HBox>
    </bottom>

</BorderPane>
```

### 5.3 FXML — Formulário de Empresa

```xml
<!-- src/main/resources/fxml/admin/EmpresaForm.fxml -->
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import atlantafx.base.controls.*?>

<ScrollPane fitToWidth="true"
            xmlns="http://javafx.com/javafx/21"
            xmlns:fx="http://javafx.com/fxml/1"
            fx:controller="ao.kubata.admin.controller.EmpresaFormController"
            styleClass="admin-form-scroll">

    <VBox spacing="16" styleClass="admin-form-container">

        <!-- ── Cabeçalho da Secção ─────────────────────────── -->
        <HBox styleClass="admin-section-header">
            <Label text="⚙ Configuração da Empresa" styleClass="admin-section-title"/>
            <Region HBox.hgrow="ALWAYS"/>
            <Button text="Guardar" fx:id="btnGuardar" onAction="#guardar"
                    styleClass="admin-btn-save"/>
            <Button text="Cancelar" onAction="#cancelar"
                    styleClass="admin-btn-cancel"/>
        </HBox>

        <!-- ── Logótipo + NIF ──────────────────────────────── -->
        <Card styleClass="admin-card">
            <HBox spacing="20" alignment="TOP_LEFT">
                <!-- Área de Logótipo -->
                <VBox spacing="8" alignment="CENTER" styleClass="admin-logo-area"
                      minWidth="160" maxWidth="160">
                    <ImageView fx:id="imgLogotipo" fitWidth="140" fitHeight="100"
                               preserveRatio="true"/>
                    <Button text="Alterar Logótipo" onAction="#alterarLogotipo"
                            styleClass="admin-btn-secondary" maxWidth="Infinity"/>
                    <Button text="Remover" onAction="#removerLogotipo"
                            styleClass="admin-btn-danger-outline" maxWidth="Infinity"/>
                </VBox>

                <!-- Dados Principais -->
                <GridPane hgap="12" vgap="8" HBox.hgrow="ALWAYS">
                    <columnConstraints>
                        <ColumnConstraints percentWidth="30"/>
                        <ColumnConstraints percentWidth="70" hgrow="ALWAYS"/>
                    </columnConstraints>

                    <Label text="NIF *" styleClass="admin-label" GridPane.rowIndex="0"/>
                    <TextField fx:id="txtNIF" promptText="Ex: 500123456" maxLength="14"
                               styleClass="admin-field" GridPane.rowIndex="0" GridPane.columnIndex="1"/>

                    <Label text="Denominação Social *" styleClass="admin-label" GridPane.rowIndex="1"/>
                    <TextField fx:id="txtDenominacao" promptText="Nome oficial da empresa"
                               styleClass="admin-field" GridPane.rowIndex="1" GridPane.columnIndex="1"/>

                    <Label text="Nome Comercial" styleClass="admin-label" GridPane.rowIndex="2"/>
                    <TextField fx:id="txtNomeComercial" promptText="Nome conhecido no mercado"
                               styleClass="admin-field" GridPane.rowIndex="2" GridPane.columnIndex="1"/>

                    <Label text="Tipo Contribuinte *" styleClass="admin-label" GridPane.rowIndex="3"/>
                    <ComboBox fx:id="cbTipoContribuinte" maxWidth="Infinity"
                              styleClass="admin-combo" GridPane.rowIndex="3" GridPane.columnIndex="1"/>

                    <Label text="Regime de IVA *" styleClass="admin-label" GridPane.rowIndex="4"/>
                    <ComboBox fx:id="cbRegimeIVA" maxWidth="Infinity"
                              styleClass="admin-combo" GridPane.rowIndex="4" GridPane.columnIndex="1"/>
                </GridPane>
            </HBox>
        </Card>

        <!-- ── Localização ─────────────────────────────────── -->
        <TitledPane text="📍 Localização e Contactos" expanded="true"
                    styleClass="admin-titled-pane">
            <GridPane hgap="12" vgap="8" styleClass="admin-grid">
                <columnConstraints>
                    <ColumnConstraints percentWidth="25"/>
                    <ColumnConstraints percentWidth="75" hgrow="ALWAYS"/>
                </columnConstraints>

                <Label text="Endereço" styleClass="admin-label" GridPane.rowIndex="0"/>
                <TextField fx:id="txtEndereco" promptText="Rua, Número, Bairro"
                           styleClass="admin-field" GridPane.rowIndex="0" GridPane.columnIndex="1"/>

                <Label text="Município" styleClass="admin-label" GridPane.rowIndex="1"/>
                <ComboBox fx:id="cbMunicipio" maxWidth="Infinity"
                          styleClass="admin-combo" GridPane.rowIndex="1" GridPane.columnIndex="1"/>

                <Label text="Província" styleClass="admin-label" GridPane.rowIndex="2"/>
                <ComboBox fx:id="cbProvincia" maxWidth="Infinity"
                          styleClass="admin-combo" GridPane.rowIndex="2" GridPane.columnIndex="1"/>

                <Label text="Telefone" styleClass="admin-label" GridPane.rowIndex="3"/>
                <TextField fx:id="txtTelefone" promptText="+244 9XX XXX XXX"
                           styleClass="admin-field" GridPane.rowIndex="3" GridPane.columnIndex="1"/>

                <Label text="E-mail" styleClass="admin-label" GridPane.rowIndex="4"/>
                <TextField fx:id="txtEmail" promptText="empresa@exemplo.ao"
                           styleClass="admin-field" GridPane.rowIndex="4" GridPane.columnIndex="1"/>

                <Label text="Website" styleClass="admin-label" GridPane.rowIndex="5"/>
                <TextField fx:id="txtWebsite" promptText="https://www.empresa.ao"
                           styleClass="admin-field" GridPane.rowIndex="5" GridPane.columnIndex="1"/>
            </GridPane>
        </TitledPane>

        <!-- ── Dados AGT ────────────────────────────────────── -->
        <TitledPane text="🏛️ Dados AGT / Registo Comercial" expanded="true"
                    styleClass="admin-titled-pane">
            <GridPane hgap="12" vgap="8" styleClass="admin-grid">
                <columnConstraints>
                    <ColumnConstraints percentWidth="25"/>
                    <ColumnConstraints percentWidth="75" hgrow="ALWAYS"/>
                </columnConstraints>

                <Label text="Código CAE" styleClass="admin-label" GridPane.rowIndex="0"/>
                <TextField fx:id="txtCAE" promptText="Código de Actividade Económica"
                           styleClass="admin-field" GridPane.rowIndex="0" GridPane.columnIndex="1"/>

                <Label text="Actividade" styleClass="admin-label" GridPane.rowIndex="1"/>
                <TextArea fx:id="txtActividade" promptText="Descrição da actividade económica"
                          prefRowCount="2" styleClass="admin-field"
                          GridPane.rowIndex="1" GridPane.columnIndex="1"/>

                <Label text="Nº Certificado AGT" styleClass="admin-label" GridPane.rowIndex="2"/>
                <TextField fx:id="txtCertificadoAGT" promptText="Número do certificado"
                           styleClass="admin-field" GridPane.rowIndex="2" GridPane.columnIndex="1"/>

                <Label text="Data Constituição" styleClass="admin-label" GridPane.rowIndex="3"/>
                <DatePicker fx:id="dpDataConstituicao" maxWidth="Infinity"
                            styleClass="admin-date-picker"
                            GridPane.rowIndex="3" GridPane.columnIndex="1"/>

                <Label text="Capital Social (AOA)" styleClass="admin-label" GridPane.rowIndex="4"/>
                <TextField fx:id="txtCapitalSocial" promptText="0,00"
                           styleClass="admin-field admin-numeric"
                           GridPane.rowIndex="4" GridPane.columnIndex="1"/>
            </GridPane>
        </TitledPane>

        <!-- ── Configuração do Software ─────────────────────── -->
        <TitledPane text="⚙️ Configurações do Sistema" expanded="false"
                    styleClass="admin-titled-pane">
            <GridPane hgap="12" vgap="8" styleClass="admin-grid">
                <columnConstraints>
                    <ColumnConstraints percentWidth="25"/>
                    <ColumnConstraints percentWidth="75" hgrow="ALWAYS"/>
                </columnConstraints>

                <Label text="Exercício Actual" styleClass="admin-label" GridPane.rowIndex="0"/>
                <Spinner fx:id="spExercicio" min="2000" max="2099"
                         styleClass="admin-spinner" GridPane.rowIndex="0" GridPane.columnIndex="1"/>

                <Label text="Moeda Base" styleClass="admin-label" GridPane.rowIndex="1"/>
                <ComboBox fx:id="cbMoedaBase" maxWidth="200"
                          styleClass="admin-combo" GridPane.rowIndex="1" GridPane.columnIndex="1"/>

                <Label text="Dec. Valores" styleClass="admin-label" GridPane.rowIndex="2"/>
                <Spinner fx:id="spDecimaisValor" min="0" max="4" initialValue="2"
                         maxWidth="100" styleClass="admin-spinner"
                         GridPane.rowIndex="2" GridPane.columnIndex="1"/>

                <Label text="Mensagem Fatura" styleClass="admin-label" GridPane.rowIndex="3"/>
                <TextArea fx:id="txtMensagemFatura"
                          promptText="Mensagem a imprimir no rodapé das faturas"
                          prefRowCount="3" styleClass="admin-field"
                          GridPane.rowIndex="3" GridPane.columnIndex="1"/>
            </GridPane>
        </TitledPane>

    </VBox>
</ScrollPane>
```

### 5.4 Controller do Formulário de Empresa

```java
// ao/kubata/admin/controller/EmpresaFormController.java
package ao.kubata.admin.controller;

import ao.kubata.admin.domain.Empresa;
import ao.kubata.admin.service.EmpresaService;
import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.Material2RoundAL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.ResourceBundle;

@Controller
public class EmpresaFormController implements Initializable {

    @Autowired private EmpresaService empresaService;

    @FXML private TextField txtNIF;
    @FXML private TextField txtDenominacao;
    @FXML private TextField txtNomeComercial;
    @FXML private ComboBox<Empresa.TipoContribuinte> cbTipoContribuinte;
    @FXML private ComboBox<Empresa.RegimeIVA> cbRegimeIVA;
    @FXML private TextField txtEndereco;
    @FXML private ComboBox<String> cbMunicipio;
    @FXML private ComboBox<String> cbProvincia;
    @FXML private TextField txtTelefone;
    @FXML private TextField txtEmail;
    @FXML private TextField txtWebsite;
    @FXML private TextField txtCAE;
    @FXML private TextArea txtActividade;
    @FXML private TextField txtCertificadoAGT;
    @FXML private DatePicker dpDataConstituicao;
    @FXML private TextField txtCapitalSocial;
    @FXML private Spinner<Integer> spExercicio;
    @FXML private ComboBox<String> cbMoedaBase;
    @FXML private Spinner<Integer> spDecimaisValor;
    @FXML private TextArea txtMensagemFatura;
    @FXML private ImageView imgLogotipo;
    @FXML private Button btnGuardar;

    private Empresa empresa;
    private byte[] logoBytes;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        carregarEnums();
        carregarProvinciasMunicipios();
        carregarEmpresa();
        configurarValidacoes();
    }

    private void carregarEnums() {
        cbTipoContribuinte.getItems().addAll(Empresa.TipoContribuinte.values());
        cbRegimeIVA.getItems().addAll(Empresa.RegimeIVA.values());
        cbMoedaBase.getItems().addAll("AOA", "USD", "EUR");
    }

    private void carregarProvinciasMunicipios() {
        cbProvincia.getItems().addAll(
            "Bengo", "Benguela", "Bié", "Cabinda", "Cuando Cubango",
            "Cuanza Norte", "Cuanza Sul", "Cunene", "Huambo", "Huíla",
            "Luanda", "Lunda Norte", "Lunda Sul", "Malanje", "Moxico",
            "Namibe", "Uíge", "Zaire"
        );

        // Luanda como default
        cbProvincia.setValue("Luanda");
        atualizarMunicipios("Luanda");

        cbProvincia.setOnAction(e -> 
            atualizarMunicipios(cbProvincia.getValue()));
    }

    private void atualizarMunicipios(String provincia) {
        cbMunicipio.getItems().clear();
        if ("Luanda".equals(provincia)) {
            cbMunicipio.getItems().addAll(
                "Belas", "Cacuaco", "Cazenga", "Icolo e Bengo",
                "Luanda", "Quilamba Kiaxi", "Talatona", "Viana"
            );
        }
        // Adicionar outros municípios por província conforme necessário
    }

    private void carregarEmpresa() {
        empresaService.obterEmpresaActual().ifPresent(e -> {
            this.empresa = e;
            preencherFormulario(e);
        });
    }

    private void preencherFormulario(Empresa e) {
        txtNIF.setText(e.getNif());
        txtDenominacao.setText(e.getDenominacaoSocial());
        txtNomeComercial.setText(e.getNomeComercial());
        cbTipoContribuinte.setValue(e.getTipoContribuinte());
        cbRegimeIVA.setValue(e.getRegimeIVA());
        txtEndereco.setText(e.getEndereco());
        cbProvincia.setValue(e.getProvincia());
        cbMunicipio.setValue(e.getMunicipio());
        txtTelefone.setText(e.getTelefone());
        txtEmail.setText(e.getEmail());
        txtWebsite.setText(e.getWebsite());
        txtCAE.setText(e.getCodigoCAE());
        txtActividade.setText(e.getDescricaoActividade());
        txtCertificadoAGT.setText(e.getNumeroCertificadoAGT());
        dpDataConstituicao.setValue(e.getDataConstituicao());
        if (e.getCapitalSocial() != null)
            txtCapitalSocial.setText(String.format("%.2f", e.getCapitalSocial()));
        spExercicio.getValueFactory().setValue(e.getExercicioActual());
        cbMoedaBase.setValue(e.getMoedaBase());
        spDecimaisValor.getValueFactory().setValue(e.getCasasDecimaisValor());
        txtMensagemFatura.setText(e.getMensagemFatura());

        // Logótipo
        if (e.getLogotipo() != null) {
            logoBytes = e.getLogotipo();
            imgLogotipo.setImage(
                new Image(new java.io.ByteArrayInputStream(logoBytes)));
        }
    }

    private void configurarValidacoes() {
        // Validação NIF em tempo real
        txtNIF.textProperty().addListener((obs, old, novo) -> {
            if (!novo.matches("\\d*")) txtNIF.setText(old);
            if (novo.length() > 14) txtNIF.setText(old);
        });

        // Só números no capital social
        txtCapitalSocial.textProperty().addListener((obs, old, novo) -> {
            if (!novo.matches("[\\d,\\.]*")) txtCapitalSocial.setText(old);
        });
    }

    @FXML
    public void guardar() {
        if (!validar()) return;

        try {
            btnGuardar.setDisable(true);
            Empresa e = empresa != null ? empresa : new Empresa();
            
            e.setNif(txtNIF.getText().trim());
            e.setDenominacaoSocial(txtDenominacao.getText().trim());
            e.setNomeComercial(txtNomeComercial.getText().trim());
            e.setTipoContribuinte(cbTipoContribuinte.getValue());
            e.setRegimeIVA(cbRegimeIVA.getValue());
            e.setEndereco(txtEndereco.getText().trim());
            e.setProvincia(cbProvincia.getValue());
            e.setMunicipio(cbMunicipio.getValue());
            e.setTelefone(txtTelefone.getText().trim());
            e.setEmail(txtEmail.getText().trim());
            e.setWebsite(txtWebsite.getText().trim());
            e.setCodigoCAE(txtCAE.getText().trim());
            e.setDescricaoActividade(txtActividade.getText().trim());
            e.setNumeroCertificadoAGT(txtCertificadoAGT.getText().trim());
            e.setDataConstituicao(dpDataConstituicao.getValue());
            e.setExercicioActual(spExercicio.getValue());
            e.setMoedaBase(cbMoedaBase.getValue());
            e.setCasasDecimaisValor(spDecimaisValor.getValue());
            e.setMensagemFatura(txtMensagemFatura.getText().trim());
            if (logoBytes != null) {
                e.setLogotipo(logoBytes);
                e.setLogotipoMimeType("image/png");
            }

            if (!txtCapitalSocial.getText().isEmpty()) {
                e.setCapitalSocial(Double.parseDouble(
                    txtCapitalSocial.getText().replace(",", ".")));
            }

            empresa = empresaService.guardar(e);
            mostrarSucesso("Empresa guardada com sucesso!");

        } catch (Exception ex) {
            mostrarErro("Erro ao guardar: " + ex.getMessage());
        } finally {
            btnGuardar.setDisable(false);
        }
    }

    private boolean validar() {
        StringBuilder erros = new StringBuilder();
        if (txtNIF.getText().trim().isEmpty())
            erros.append("• NIF é obrigatório\n");
        if (txtDenominacao.getText().trim().isEmpty())
            erros.append("• Denominação Social é obrigatória\n");
        if (cbTipoContribuinte.getValue() == null)
            erros.append("• Tipo de Contribuinte é obrigatório\n");
        if (cbRegimeIVA.getValue() == null)
            erros.append("• Regime de IVA é obrigatório\n");

        if (erros.length() > 0) {
            mostrarErro("Por favor corrija:\n" + erros);
            return false;
        }
        return true;
    }

    @FXML
    public void alterarLogotipo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar Logótipo");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File file = fc.showOpenDialog(imgLogotipo.getScene().getWindow());
        if (file != null) {
            try {
                logoBytes = Files.readAllBytes(file.toPath());
                imgLogotipo.setImage(new Image(file.toURI().toString()));
            } catch (IOException e) {
                mostrarErro("Não foi possível carregar a imagem");
            }
        }
    }

    @FXML
    public void removerLogotipo() {
        logoBytes = null;
        imgLogotipo.setImage(null);
    }

    @FXML
    public void cancelar() {
        carregarEmpresa(); // Recarregar dados originais
    }

    private void mostrarSucesso(String msg) {
        // Usar AtlantaFX Notification ou Alert
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.setHeaderText("Sucesso");
        alert.showAndWait();
    }

    private void mostrarErro(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        alert.setHeaderText("Erro");
        alert.showAndWait();
    }
}
```

---

## 6. Páginas e Navegação (TabPane + Ribbon)

### 6.1 Dashboard do Administrator — KPI Cards

```java
// ao/kubata/admin/controller/AdminDashboardController.java
package ao.kubata.admin.controller;

import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

@Controller
public class AdminDashboardController implements Initializable {

    @Autowired private UtilizadorService utilizadorService;
    @Autowired private LogAuditoriaRepository logRepo;

    @FXML private FlowPane kpiContainer;
    @FXML private BarChart<String, Number> chartLoginsPorDia;
    @FXML private TableView<?> tblUltimosLogs;
    @FXML private Label lblUltimaActualizacao;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        carregarKPIs();
        carregarGraficoLogins();
        carregarUltimosLogs();
        lblUltimaActualizacao.setText("Actualizado: " +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
    }

    private void carregarKPIs() {
        long totalUtilizadores = utilizadorService.listarTodos().size();
        long utilizadoresActivos = utilizadorService.listarTodos().stream()
            .filter(u -> u.getEstado() == Utilizador.EstadoUtilizador.ACTIVO).count();
        long utilizadoresBloqueados = utilizadorService.listarTodos().stream()
            .filter(u -> u.getEstado() == Utilizador.EstadoUtilizador.BLOQUEADO).count();
        long totalLogs = logRepo.count();

        kpiContainer.getChildren().addAll(
            criarKPI("Utilizadores", String.valueOf(totalUtilizadores),
                "mdi2a-account-group", "#1B5E20"),
            criarKPI("Activos", String.valueOf(utilizadoresActivos),
                "mdi2a-account-check", "#2E7D32"),
            criarKPI("Bloqueados", String.valueOf(utilizadoresBloqueados),
                "mdi2a-account-lock", "#C62828"),
            criarKPI("Logs de Auditoria", String.valueOf(totalLogs),
                "mdi2c-clipboard-list", "#1565C0")
        );
    }

    private VBox criarKPI(String titulo, String valor,
                           String icone, String corBorda) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("admin-kpi-card");
        card.setStyle("-fx-border-color: " + corBorda + ";");
        card.setPrefWidth(180);

        FontIcon icon = new FontIcon(icone);
        icon.setIconSize(32);
        icon.getStyleClass().add("admin-kpi-icon");

        Label lblValor = new Label(valor);
        lblValor.getStyleClass().add("admin-kpi-valor");

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("admin-kpi-titulo");

        card.getChildren().addAll(icon, lblValor, lblTitulo);
        return card;
    }

    private void carregarGraficoLogins() {
        // Implementar com dados reais da base de dados
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Logins");
        // Adicionar dados dos últimos 7 dias
        chartLoginsPorDia.getData().add(series);
    }

    private void carregarUltimosLogs() {
        // Carregar os últimos 20 registos de auditoria
    }
}
```

### 6.2 FXML Dashboard

```xml
<!-- src/main/resources/fxml/admin/AdminDashboard.fxml -->
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.chart.*?>

<VBox spacing="16" styleClass="admin-dashboard"
      xmlns="http://javafx.com/javafx/21"
      xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="ao.kubata.admin.controller.AdminDashboardController">

    <!-- KPIs -->
    <Label text="📊 Visão Geral do Sistema" styleClass="admin-section-title"/>
    <FlowPane fx:id="kpiContainer" hgap="12" vgap="12"/>

    <!-- Gráficos + Logs em Split -->
    <SplitPane dividerPositions="0.5" VBox.vgrow="ALWAYS">
        <BarChart fx:id="chartLoginsPorDia" title="Logins por Dia (Últimos 7 dias)"
                  styleClass="admin-chart">
            <xAxis><CategoryAxis label="Dia"/></xAxis>
            <yAxis><NumberAxis label="Logins"/></yAxis>
        </BarChart>

        <VBox spacing="8">
            <Label text="Últimas Operações" styleClass="admin-sub-section-title"/>
            <TableView fx:id="tblUltimosLogs" VBox.vgrow="ALWAYS">
                <columns>
                    <TableColumn text="Data/Hora" prefWidth="140"/>
                    <TableColumn text="Utilizador" prefWidth="120"/>
                    <TableColumn text="Operação" prefWidth="120"/>
                    <TableColumn text="Descrição" prefWidth="250"/>
                </columns>
            </TableView>
        </VBox>
    </SplitPane>

    <HBox alignment="CENTER_RIGHT">
        <Label fx:id="lblUltimaActualizacao" styleClass="admin-hint-label"/>
    </HBox>
</VBox>
```

---

## 7. Sistema de Permissões e Perfis

### 7.1 Serviço de Verificação de Permissões

```java
// ao/kubata/admin/service/PermissaoService.java
package ao.kubata.admin.service;

import ao.kubata.admin.domain.*;
import ao.kubata.admin.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissaoService {

    private final PerfilAcessoRepository perfilRepository;

    /**
     * Verifica se o utilizador tem permissão para uma operação específica.
     * 
     * Exemplo de uso:
     *   permissaoService.temPermissao(utilizador, "FATURACAO", "FATURAS", Operacao.CRIAR)
     */
    public boolean temPermissao(Utilizador utilizador, String modulo,
                                  String recurso, PermissaoPerfil.Operacao operacao) {
        // SuperAdmin tem tudo
        if (Boolean.TRUE.equals(utilizador.getSuperAdmin())) return true;

        return utilizador.getPerfis().stream()
            .filter(p -> Boolean.TRUE.equals(p.getActivo()))
            .flatMap(p -> p.getPermissoes().stream())
            .anyMatch(pm ->
                pm.getModulo().equalsIgnoreCase(modulo) &&
                pm.getRecurso().equalsIgnoreCase(recurso) &&
                pm.getOperacao() == operacao &&
                Boolean.TRUE.equals(pm.getPermitido())
            );
    }

    /**
     * Obtém todos os módulos acessíveis ao utilizador.
     */
    public Set<String> modulosAcessiveis(Utilizador utilizador) {
        if (Boolean.TRUE.equals(utilizador.getSuperAdmin())) {
            // Retornar todos os módulos
            return Set.of("ADMINISTRATOR", "FATURACAO", "STOCK",
                          "CLIENTES", "RH", "RELATORIOS");
        }
        return utilizador.getPerfis().stream()
            .filter(p -> Boolean.TRUE.equals(p.getActivo()))
            .flatMap(p -> p.getPermissoes().stream())
            .filter(pm -> Boolean.TRUE.equals(pm.getPermitido()))
            .map(PermissaoPerfil::getModulo)
            .collect(Collectors.toSet());
    }

    /**
     * Inicializa os perfis padrão do sistema (chamado na instalação)
     */
    public void inicializarPerfisPadrao() {
        criarPerfilSuperAdmin();
        criarPerfilAdministrador();
        criarPerfilFaturador();
        criarPerfilConsultor();
    }

    private void criarPerfilSuperAdmin() {
        PerfilAcesso perfil = PerfilAcesso.builder()
            .codigo("SUPERADMIN")
            .descricao("Super Administrador")
            .sistema(true)
            .build();
        // ... adicionar todas as permissões
        perfilRepository.save(perfil);
    }

    private void criarPerfilAdministrador() {
        PerfilAcesso perfil = PerfilAcesso.builder()
            .codigo("ADMIN")
            .descricao("Administrador do Sistema")
            .sistema(true)
            .build();
        // Permissões de gestão de utilizadores e configuração
        adicionarPermissoes(perfil, "ADMINISTRATOR",
            new String[]{"UTILIZADORES", "PERFIS", "SERIES", "EMPRESA"},
            PermissaoPerfil.Operacao.values());
        perfilRepository.save(perfil);
    }

    private void criarPerfilFaturador() {
        PerfilAcesso perfil = PerfilAcesso.builder()
            .codigo("FATURADOR")
            .descricao("Operador de Faturação")
            .sistema(true)
            .build();
        adicionarPermissoes(perfil, "FATURACAO",
            new String[]{"FATURAS", "CLIENTES"},
            new PermissaoPerfil.Operacao[]{
                PermissaoPerfil.Operacao.VER,
                PermissaoPerfil.Operacao.CRIAR,
                PermissaoPerfil.Operacao.IMPRIMIR
            });
        perfilRepository.save(perfil);
    }

    private void criarPerfilConsultor() {
        PerfilAcesso perfil = PerfilAcesso.builder()
            .codigo("CONSULTOR")
            .descricao("Consultor (Apenas Leitura)")
            .sistema(true)
            .build();
        // Apenas VER em tudo
        perfilRepository.save(perfil);
    }

    private void adicionarPermissoes(PerfilAcesso perfil, String modulo,
                                      String[] recursos, PermissaoPerfil.Operacao[] ops) {
        for (String recurso : recursos) {
            for (PermissaoPerfil.Operacao op : ops) {
                perfil.getPermissoes().add(PermissaoPerfil.builder()
                    .perfil(perfil)
                    .modulo(modulo)
                    .recurso(recurso)
                    .operacao(op)
                    .permitido(true)
                    .build());
            }
        }
    }
}
```

### 7.2 Anotação Personalizada para Controlo de Acesso

```java
// ao/kubata/admin/security/RequerePermissao.java
package ao.kubata.admin.security;

import ao.kubata.admin.domain.PermissaoPerfil.Operacao;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequerePermissao {
    String modulo();
    String recurso();
    Operacao operacao();
}
```

```java
// ao/kubata/admin/security/PermissaoAspect.java
package ao.kubata.admin.security;

import ao.kubata.admin.service.PermissaoService;
import ao.kubata.admin.session.SessaoActual;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class PermissaoAspect {

    private final PermissaoService permissaoService;
    private final SessaoActual sessaoActual;

    @Around("@annotation(RequerePermissao)")
    public Object verificarPermissao(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        RequerePermissao anotacao = sig.getMethod().getAnnotation(RequerePermissao.class);

        if (!permissaoService.temPermissao(
                sessaoActual.getUtilizador(),
                anotacao.modulo(),
                anotacao.recurso(),
                anotacao.operacao())) {
            throw new SecurityException(
                "Não tem permissão para " + anotacao.operacao() +
                " em " + anotacao.recurso());
        }
        return pjp.proceed();
    }
}
```

---

## 8. Configuração Fiscal Angolana (AGT)

### 8.1 Taxas de IVA Angolanas

```java
// ao/kubata/admin/domain/TaxaIVA.java
package ao.kubata.admin.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "adm_taxa_iva")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxaIVA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 10)
    private String codigo;  // IVA14, ISE, RED7

    @Column(name = "descricao", nullable = false, length = 100)
    private String descricao;

    @Column(name = "percentagem", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentagem;  // 14.00, 7.00, 0.00

    @Column(name = "codigo_saft", length = 10)
    private String codigoSAFT;  // NOR, ISE, RED

    @Column(name = "vigente_desde")
    private LocalDate vigentDesde;

    @Column(name = "activa")
    @Builder.Default
    private Boolean activa = true;

    @Column(name = "padrao")
    @Builder.Default
    private Boolean padrao = false;
}
```

```java
// Dados de inicialização das taxas de IVA angolanas
// ao/kubata/admin/service/TaxaIVAService.java
public void inicializarTaxasIVA() {
    List<TaxaIVA> taxas = Arrays.asList(
        TaxaIVA.builder()
            .codigo("IVA14")
            .descricao("IVA Taxa Normal - 14%")
            .percentagem(new BigDecimal("14.00"))
            .codigoSAFT("NOR")
            .vigentDesde(LocalDate.of(2019, 10, 1))
            .padrao(true)
            .build(),
        TaxaIVA.builder()
            .codigo("ISE")
            .descricao("Isento de IVA")
            .percentagem(BigDecimal.ZERO)
            .codigoSAFT("ISE")
            .vigentDesde(LocalDate.of(2019, 10, 1))
            .build(),
        TaxaIVA.builder()
            .codigo("RED7")
            .descricao("IVA Taxa Reduzida - 7%")
            .percentagem(new BigDecimal("7.00"))
            .codigoSAFT("RED")
            .vigentDesde(LocalDate.of(2019, 10, 1))
            .build(),
        TaxaIVA.builder()
            .codigo("OUT")
            .descricao("Fora do Âmbito do IVA")
            .percentagem(BigDecimal.ZERO)
            .codigoSAFT("OUT")
            .vigentDesde(LocalDate.of(2019, 10, 1))
            .build()
    );
    taxaIVARepository.saveAll(taxas);
}
```

### 8.2 Parâmetros Fiscais AGT

```java
// Parâmetros padrão do sistema fiscal angolano
public void inicializarParametrosFiscais(Empresa empresa) {
    List<ParametroSistema> params = Arrays.asList(
        // IVA
        param(empresa, "FISCAL.IVA_PADRAO", "14.00", "DECIMAL",
              "Taxa de IVA padrão em Angola (%)", "FISCAL"),
        param(empresa, "FISCAL.REGIME_IVA", "GERAL", "STRING",
              "Regime de IVA: GERAL, SIMPLIFICADO, ISENTO", "FISCAL"),

        // SAF-T
        param(empresa, "SAFT.VERSAO", "1.04_01", "STRING",
              "Versão do ficheiro SAF-T AO", "SAFT"),
        param(empresa, "SAFT.NUMERO_ENTRADAS_HASH", "4", "INTEGER",
              "Número de entradas para cálculo de hash encadeado", "SAFT"),

        // Documentos
        param(empresa, "DOC.DIAS_VENCIMENTO", "30", "INTEGER",
              "Prazo de vencimento padrão em dias", "FISCAL"),
        param(empresa, "DOC.HASH_ALGORITMO", "SHA-256", "STRING",
              "Algoritmo de hash para assinatura digital", "FISCAL"),

        // Moeda
        param(empresa, "MOEDA.SIMBOLO", "Kz", "STRING",
              "Símbolo da moeda base (Kwanza)", "FINANCEIRO"),
        param(empresa, "MOEDA.CODIGO_ISO", "AOA", "STRING",
              "Código ISO da moeda base", "FINANCEIRO"),
        param(empresa, "MOEDA.CASAS_DECIMAIS", "2", "INTEGER",
              "Casas decimais nos valores monetários", "FINANCEIRO"),

        // Interface
        param(null, "UI.TEMA", "VERDE_ADMIN", "STRING",
              "Tema da interface", "INTERFACE"),
        param(null, "UI.IDIOMA", "pt-AO", "STRING",
              "Idioma da interface", "INTERFACE"),
        param(null, "UI.LINHAS_PAGINA", "50", "INTEGER",
              "Linhas por página nas tabelas", "INTERFACE")
    );
    parametroRepository.saveAll(params);
}

private ParametroSistema param(Empresa empresa, String chave, String valor,
                               String tipo, String descricao, String grupo) {
    return ParametroSistema.builder()
        .empresa(empresa)
        .chave(chave)
        .valor(valor)
        .tipoValor(tipo)
        .descricao(descricao)
        .grupo(grupo)
        .editavel(true)
        .build();
}
```

---

## 9. Séries de Documentos SAF-T AO

### 9.1 FXML — Gestão de Séries

```xml
<!-- src/main/resources/fxml/admin/SeriesDocumento.fxml -->
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<VBox spacing="12" styleClass="admin-list-view"
      xmlns="http://javafx.com/javafx/21"
      xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="ao.kubata.admin.controller.SeriesController">

    <!-- Toolbar -->
    <HBox styleClass="admin-toolbar" spacing="8" alignment="CENTER_LEFT">
        <Label text="📄 Séries de Documentos" styleClass="admin-section-title"/>
        <Region HBox.hgrow="ALWAYS"/>
        <Button text="➕ Nova Série" onAction="#novaSerie" styleClass="admin-btn-primary"/>
        <Button text="🔄 Inicializar Séries" onAction="#inicializarSeries"
                styleClass="admin-btn-secondary"/>
    </HBox>

    <!-- Filtros por tipo -->
    <HBox spacing="8" alignment="CENTER_LEFT" styleClass="admin-filter-bar">
        <Label text="Filtrar por área:"/>
        <ToggleButton text="Todos" fx:id="togTodos" selected="true"/>
        <ToggleButton text="Vendas" fx:id="togVendas"/>
        <ToggleButton text="Stock" fx:id="togStock"/>
        <ToggleButton text="Financeiro" fx:id="togFinanceiro"/>
        <ToggleButton text="Compras" fx:id="togCompras"/>
    </HBox>

    <!-- Tabela de Séries -->
    <TableView fx:id="tblSeries" VBox.vgrow="ALWAYS"
               styleClass="admin-table" placeholder="Nenhuma série configurada">
        <columns>
            <TableColumn text="Tipo" fx:id="colTipo" prefWidth="160"/>
            <TableColumn text="Prefixo" fx:id="colPrefixo" prefWidth="70"/>
            <TableColumn text="Série" fx:id="colSerie" prefWidth="80"/>
            <TableColumn text="Descrição" fx:id="colDescricao" prefWidth="200"/>
            <TableColumn text="Último Nº" fx:id="colUltimoNum" prefWidth="100"/>
            <TableColumn text="Exercício" fx:id="colExercicio" prefWidth="90"/>
            <TableColumn text="Estado" fx:id="colEstado" prefWidth="120"/>
            <TableColumn text="Predefinida" fx:id="colPredefinida" prefWidth="100"/>
            <TableColumn text="AGT" fx:id="colAGT" prefWidth="70"/>
            <TableColumn text="Acções" fx:id="colAccoes" prefWidth="100"/>
        </columns>
    </TableView>

    <!-- Legenda -->
    <HBox spacing="16" styleClass="admin-legend">
        <Label text="🟢 Activa" styleClass="admin-legend-active"/>
        <Label text="⚫ Inactiva" styleClass="admin-legend-inactive"/>
        <Label text="🔒 Encerrada" styleClass="admin-legend-closed"/>
        <Label text="⭐ Predefinida" styleClass="admin-legend-default"/>
    </HBox>
</VBox>
```

---

## 10. Gestão de Módulos (Plugin System)

### 10.1 FXML — Instalação de Módulos

```xml
<!-- src/main/resources/fxml/admin/ModulosSistema.fxml -->
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<VBox spacing="12" styleClass="admin-modulos-view"
      xmlns="http://javafx.com/javafx/21"
      xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="ao.kubata.admin.controller.ModulosController">

    <Label text="🧩 Módulos do Sistema" styleClass="admin-section-title"/>

    <!-- Grid de módulos -->
    <FlowPane fx:id="modulosContainer" hgap="12" vgap="12"
              VBox.vgrow="ALWAYS" styleClass="admin-modulos-grid"/>
</VBox>
```

### 10.2 Controller de Módulos com Cards

```java
// ao/kubata/admin/controller/ModulosController.java
package ao.kubata.admin.controller;

import ao.kubata.admin.domain.ModuloSistema;
import ao.kubata.admin.service.ModuloSistemaService;
import atlantafx.base.controls.Card;
import atlantafx.base.controls.Tile;
import atlantafx.base.theme.Styles;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Controller
public class ModulosController implements Initializable {

    @Autowired private ModuloSistemaService moduloService;

    @FXML private FlowPane modulosContainer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        carregarModulos();
    }

    private void carregarModulos() {
        List<ModuloSistema> modulos = moduloService.listarTodos();
        modulosContainer.getChildren().clear();

        for (ModuloSistema modulo : modulos) {
            modulosContainer.getChildren().add(criarCardModulo(modulo));
        }
    }

    private VBox criarCardModulo(ModuloSistema modulo) {
        VBox card = new VBox(12);
        card.getStyleClass().add("admin-modulo-card");
        card.setPrefWidth(220);
        card.setPrefHeight(200);

        // Estado badge
        Label badge = new Label(modulo.getEstado().toString());
        badge.getStyleClass().addAll("admin-modulo-badge",
            modulo.getEstado() == ModuloSistema.EstadoModulo.ACTIVO
                ? "badge-success" : "badge-default");

        // Ícone
        FontIcon icon = new FontIcon(modulo.getIconeClasse() != null
            ? modulo.getIconeClasse() : "mdi2p-puzzle");
        icon.setIconSize(48);
        icon.setIconColor(Color.web(modulo.getCorHex() != null
            ? modulo.getCorHex() : "#4CAF50"));

        // Nome e descrição
        Label lblNome = new Label(modulo.getNome());
        lblNome.getStyleClass().add("admin-modulo-nome");

        Label lblVersao = new Label("v" + modulo.getVersao());
        lblVersao.getStyleClass().add("admin-modulo-versao");

        Label lblDesc = new Label(modulo.getDescricao());
        lblDesc.getStyleClass().add("admin-modulo-desc");
        lblDesc.setWrapText(true);

        // Botão de acção
        Button btn = new Button();
        btn.setMaxWidth(Double.MAX_VALUE);

        if (modulo.getEstado() == ModuloSistema.EstadoModulo.ACTIVO) {
            btn.setText("⏹ Desactivar");
            btn.getStyleClass().add("admin-btn-danger-outline");
            btn.setOnAction(e -> desactivarModulo(modulo));
        } else if (modulo.getEstado() == ModuloSistema.EstadoModulo.DISPONIVEL) {
            btn.setText("▶ Activar");
            btn.getStyleClass().add("admin-btn-success");
            btn.setOnAction(e -> activarModulo(modulo));
        }

        if (Boolean.TRUE.equals(modulo.getObrigatorio())) {
            btn.setDisable(true);
            btn.setText("🔒 Obrigatório");
        }

        card.getChildren().addAll(
            new HBox(badge), icon, lblNome, lblVersao, lblDesc, btn);
        return card;
    }

    private void activarModulo(ModuloSistema modulo) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Activar o módulo '" + modulo.getNome() + "'?");
        confirm.setHeaderText("Activar Módulo");
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                moduloService.activar(modulo.getId());
                carregarModulos();
            }
        });
    }

    private void desactivarModulo(ModuloSistema modulo) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Desactivar '" + modulo.getNome() + "'? Os utilizadores perderão acesso.",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("⚠ Confirmar Desactivação");
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                moduloService.desactivar(modulo.getId());
                carregarModulos();
            }
        });
    }
}
```

---

## 11. Auditoria e Logs do Sistema

### 11.1 Serviço de Auditoria com AOP

```java
// ao/kubata/admin/service/AuditoriaService.java
package ao.kubata.admin.service;

import ao.kubata.admin.domain.LogAuditoria;
import ao.kubata.admin.repository.LogAuditoriaRepository;
import ao.kubata.admin.session.SessaoActual;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoriaService {

    private final LogAuditoriaRepository logRepository;
    private final SessaoActual sessaoActual;
    private final ObjectMapper objectMapper;

    @Async  // Não bloquear a thread principal
    public void registar(LogAuditoria.TipoOperacao operacao,
                         String modulo, String entidade, String entidadeId,
                         String descricao, Object dadosAnteriores, Object dadosNovos) {
        try {
            LogAuditoria log = LogAuditoria.builder()
                .operacao(operacao)
                .modulo(modulo)
                .entidade(entidade)
                .entidadeId(entidadeId)
                .descricao(descricao)
                .username(sessaoActual.getUsername())
                .utilizadorId(sessaoActual.getUtilizadorId())
                .resultado(LogAuditoria.ResultadoOperacao.SUCESSO)
                .build();

            if (dadosAnteriores != null) {
                log.setDadosAnteriores(objectMapper.writeValueAsString(dadosAnteriores));
            }
            if (dadosNovos != null) {
                log.setDadosNovos(objectMapper.writeValueAsString(dadosNovos));
            }

            logRepository.save(log);
        } catch (Exception e) {
            // Auditoria nunca deve quebrar o fluxo principal
            log.error("Erro ao registar auditoria: {}", e.getMessage());
        }
    }

    public void registarErro(LogAuditoria.TipoOperacao operacao, String modulo,
                              String descricao, String mensagemErro) {
        try {
            LogAuditoria auditoria = LogAuditoria.builder()
                .operacao(operacao)
                .modulo(modulo)
                .descricao(descricao)
                .mensagemErro(mensagemErro)
                .username(sessaoActual.getUsername())
                .resultado(LogAuditoria.ResultadoOperacao.FALHA)
                .build();
            logRepository.save(auditoria);
        } catch (Exception e) {
            log.error("Falha crítica no registo de auditoria: {}", e.getMessage());
        }
    }
}
```

### 11.2 View de Logs — FXML

```xml
<!-- src/main/resources/fxml/admin/Auditoria.fxml -->
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.cell.*?>

<VBox spacing="12" styleClass="admin-auditoria-view"
      xmlns="http://javafx.com/javafx/21"
      xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="ao.kubata.admin.controller.AuditoriaController">

    <Label text="📋 Registo de Auditoria" styleClass="admin-section-title"/>

    <!-- Filtros avançados -->
    <TitledPane text="🔍 Filtros" expanded="true" styleClass="admin-filter-pane">
        <GridPane hgap="12" vgap="8">
            <columnConstraints>
                <ColumnConstraints percentWidth="20"/>
                <ColumnConstraints percentWidth="30"/>
                <ColumnConstraints percentWidth="20"/>
                <ColumnConstraints percentWidth="30"/>
            </columnConstraints>

            <Label text="Utilizador:" GridPane.rowIndex="0"/>
            <TextField fx:id="txtFiltroUser" promptText="Pesquisar por utilizador"
                       GridPane.rowIndex="0" GridPane.columnIndex="1"/>

            <Label text="Operação:" GridPane.rowIndex="0" GridPane.columnIndex="2"/>
            <ComboBox fx:id="cbFiltroOp" promptText="Todas"
                      GridPane.rowIndex="0" GridPane.columnIndex="3" maxWidth="Infinity"/>

            <Label text="Data Início:" GridPane.rowIndex="1"/>
            <DatePicker fx:id="dpInicio" GridPane.rowIndex="1" GridPane.columnIndex="1"
                        maxWidth="Infinity"/>

            <Label text="Data Fim:" GridPane.rowIndex="1" GridPane.columnIndex="2"/>
            <DatePicker fx:id="dpFim" GridPane.rowIndex="1" GridPane.columnIndex="3"
                        maxWidth="Infinity"/>

            <Label text="Módulo:" GridPane.rowIndex="2"/>
            <ComboBox fx:id="cbFiltroModulo" promptText="Todos os módulos"
                      GridPane.rowIndex="2" GridPane.columnIndex="1" maxWidth="Infinity"/>

            <HBox spacing="8" GridPane.rowIndex="2" GridPane.columnIndex="3">
                <Button text="🔍 Pesquisar" onAction="#pesquisar"
                        styleClass="admin-btn-primary"/>
                <Button text="🧹 Limpar" onAction="#limparFiltros"
                        styleClass="admin-btn-secondary"/>
                <Button text="📥 Exportar" onAction="#exportar"
                        styleClass="admin-btn-secondary"/>
            </HBox>
        </GridPane>
    </TitledPane>

    <!-- Tabela de logs -->
    <TableView fx:id="tblLogs" VBox.vgrow="ALWAYS" styleClass="admin-table">
        <columns>
            <TableColumn text="Data / Hora" prefWidth="155"/>
            <TableColumn text="Utilizador" prefWidth="120"/>
            <TableColumn text="Módulo" prefWidth="120"/>
            <TableColumn text="Operação" prefWidth="130"/>
            <TableColumn text="Entidade" prefWidth="120"/>
            <TableColumn text="Descrição" prefWidth="300"/>
            <TableColumn text="IP" prefWidth="120"/>
            <TableColumn text="Resultado" prefWidth="100"/>
        </columns>
        <placeholder>
            <Label text="Nenhum registo encontrado com os filtros aplicados"
                   styleClass="admin-placeholder"/>
        </placeholder>
    </TableView>

    <!-- Paginação -->
    <HBox alignment="CENTER" spacing="8">
        <Pagination fx:id="paginacao" maxPageIndicatorCount="10"/>
    </HBox>
</VBox>
```

---

## 12. Backup e Manutenção da Base de Dados

### 12.1 Serviço de Backup

```java
// ao/kubata/admin/service/BackupService.java
package ao.kubata.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    @Value("${kubata.backup.diretorio:backups}")
    private String diretorioBackup;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private final AuditoriaService auditoriaService;

    /**
     * Backup automático diário às 02:00
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void backupAutomaticoNocturno() {
        try {
            String caminho = executarBackup("AUTO");
            log.info("Backup automático realizado: {}", caminho);
            auditoriaService.registar(
                ao.kubata.admin.domain.LogAuditoria.TipoOperacao.BACKUP,
                "SISTEMA", "Backup", null,
                "Backup automático nocturno: " + caminho, null, null);
        } catch (Exception e) {
            log.error("Falha no backup automático: {}", e.getMessage());
        }
    }

    /**
     * Backup manual iniciado pelo administrador
     */
    public String executarBackupManual() throws IOException, InterruptedException {
        return executarBackup("MANUAL");
    }

    private String executarBackup(String tipo) throws IOException, InterruptedException {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nomeArquivo = "kubata_backup_" + tipo + "_" + timestamp + ".sql";

        Path dirPath = Paths.get(diretorioBackup);
        Files.createDirectories(dirPath);

        Path arquivoBackup = dirPath.resolve(nomeArquivo);

        // Extrair dados do URL do datasource
        // jdbc:mysql://localhost:3306/kubata_db
        String dbName = extrairNomeBD(datasourceUrl);
        String host = extrairHost(datasourceUrl);

        ProcessBuilder pb = new ProcessBuilder(
            "mysqldump",
            "-h", host,
            "-u", dbUser,
            "-p" + dbPassword,
            "--single-transaction",
            "--routines",
            "--triggers",
            dbName
        );

        pb.redirectOutput(arquivoBackup.toFile());
        pb.redirectErrorStream(false);

        Process processo = pb.start();
        int exitCode = processo.waitFor();

        if (exitCode != 0) {
            throw new IOException("mysqldump falhou com código " + exitCode);
        }

        // Comprimir em ZIP
        String zipPath = comprimirBackup(arquivoBackup);
        Files.deleteIfExists(arquivoBackup);  // Remover o .sql, manter apenas .zip

        log.info("Backup criado: {} ({} MB)",
            zipPath, Files.size(Paths.get(zipPath)) / 1024 / 1024);

        return zipPath;
    }

    private String comprimirBackup(Path sqlFile) throws IOException {
        String zipPath = sqlFile.toString().replace(".sql", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(
                new FileOutputStream(zipPath));
             FileInputStream fis = new FileInputStream(sqlFile.toFile())) {

            ZipEntry entry = new ZipEntry(sqlFile.getFileName().toString());
            zos.putNextEntry(entry);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            zos.closeEntry();
        }
        return zipPath;
    }

    public void limparBackupsAntigos(int diasRetencao) throws IOException {
        Path dirPath = Paths.get(diretorioBackup);
        LocalDateTime limite = LocalDateTime.now().minusDays(diasRetencao);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.zip")) {
            for (Path backup : stream) {
                if (Files.getLastModifiedTime(backup).toInstant()
                        .isBefore(limite.atZone(java.time.ZoneId.systemDefault())
                        .toInstant())) {
                    Files.delete(backup);
                    log.info("Backup antigo removido: {}", backup.getFileName());
                }
            }
        }
    }

    private String extrairNomeBD(String url) {
        // jdbc:mysql://localhost:3306/kubata_db
        return url.substring(url.lastIndexOf('/') + 1).split("\\?")[0];
    }

    private String extrairHost(String url) {
        // jdbc:mysql://localhost:3306/kubata_db -> localhost
        String sem = url.replace("jdbc:mysql://", "");
        return sem.substring(0, sem.indexOf(':'));
    }
}
```

### 12.2 View de Backup — FXML

```xml
<!-- src/main/resources/fxml/admin/BackupRestaurar.fxml -->
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<VBox spacing="16" styleClass="admin-backup-view"
      xmlns="http://javafx.com/javafx/21"
      xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="ao.kubata.admin.controller.BackupController">

    <Label text="💾 Backup e Restauro" styleClass="admin-section-title"/>

    <HBox spacing="16">
        <!-- ── Coluna Esquerda: Backup ─────────────────── -->
        <VBox spacing="12" HBox.hgrow="ALWAYS" styleClass="admin-card">
            <Label text="📤 Criar Backup" styleClass="admin-card-title"/>

            <GridPane hgap="12" vgap="8">
                <columnConstraints>
                    <ColumnConstraints percentWidth="40"/>
                    <ColumnConstraints percentWidth="60" hgrow="ALWAYS"/>
                </columnConstraints>

                <Label text="Diretório de Destino:" GridPane.rowIndex="0"/>
                <HBox spacing="4" GridPane.rowIndex="0" GridPane.columnIndex="1">
                    <TextField fx:id="txtDirBackup" HBox.hgrow="ALWAYS"/>
                    <Button text="..." onAction="#seleccionarDiretorio"/>
                </HBox>

                <Label text="Backup Automático:" GridPane.rowIndex="1"/>
                <CheckBox fx:id="chkBackupAuto" text="Activo (diário às 02:00)"
                          GridPane.rowIndex="1" GridPane.columnIndex="1"/>

                <Label text="Retenção (dias):" GridPane.rowIndex="2"/>
                <Spinner fx:id="spRetencao" min="1" max="365" initialValue="30"
                         GridPane.rowIndex="2" GridPane.columnIndex="1" maxWidth="100"/>

                <Label text="Incluir:" GridPane.rowIndex="3"/>
                <VBox GridPane.rowIndex="3" GridPane.columnIndex="1" spacing="4">
                    <CheckBox text="Dados (tabelas)" selected="true" disable="true"/>
                    <CheckBox text="Estrutura (DDL)" selected="true" disable="true"/>
                    <CheckBox text="Procedures e Triggers" fx:id="chkProcedures"/>
                    <CheckBox text="Compresão ZIP" fx:id="chkComprimir" selected="true"/>
                </VBox>
            </GridPane>

            <Button text="▶ Iniciar Backup Agora" onAction="#iniciarBackup"
                    styleClass="admin-btn-primary" maxWidth="Infinity"/>

            <ProgressBar fx:id="pbBackup" maxWidth="Infinity" visible="false"/>
            <Label fx:id="lblStatusBackup" styleClass="admin-status-text"/>
        </VBox>

        <!-- ── Coluna Direita: Restauro + Histórico ──────── -->
        <VBox spacing="12" HBox.hgrow="ALWAYS" styleClass="admin-card">
            <Label text="📥 Restaurar Backup" styleClass="admin-card-title"/>

            <Label text="⚠️ ATENÇÃO: O restauro irá substituir todos os dados actuais!"
                   styleClass="admin-warning-label" wrapText="true"/>

            <HBox spacing="4">
                <TextField fx:id="txtFicheiroRestaurar" HBox.hgrow="ALWAYS"
                           promptText="Seleccione o ficheiro .zip de backup"/>
                <Button text="📂" onAction="#seleccionarFicheiroBackup"/>
            </HBox>

            <Button text="⚠ Restaurar" onAction="#restaurarBackup"
                    styleClass="admin-btn-danger" maxWidth="Infinity"/>

            <Separator/>

            <Label text="📋 Backups Disponíveis" styleClass="admin-sub-section-title"/>
            <TableView fx:id="tblBackups" prefHeight="250">
                <columns>
                    <TableColumn text="Nome do Ficheiro" prefWidth="200"/>
                    <TableColumn text="Data" prefWidth="140"/>
                    <TableColumn text="Tamanho" prefWidth="90"/>
                    <TableColumn text="Tipo" prefWidth="80"/>
                    <TableColumn text="Acções" prefWidth="100"/>
                </columns>
            </TableView>
        </VBox>
    </HBox>
</VBox>
```

---

## 13. CSS — Tema Verde Excel 365 para o Administrator

```css
/* src/main/resources/css/admin/admin-theme.css */
/* ═══════════════════════════════════════════════════════════════════
   KUBATA ADMINISTRATOR — TEMA VERDE PROFISSIONAL (Estilo Excel 365)
   Paleta: Verde Escuro #1B5E20 | Verde Médio #2E7D32 | Verde Claro #4CAF50
   ═══════════════════════════════════════════════════════════════════ */

/* ── Variáveis Globais ─────────────────────────────────────────── */
.root {
    /* Verdes principais */
    -admin-green-900: #1B5E20;
    -admin-green-800: #2E7D32;
    -admin-green-700: #388E3C;
    -admin-green-600: #43A047;
    -admin-green-500: #4CAF50;
    -admin-green-400: #66BB6A;
    -admin-green-300: #81C784;
    -admin-green-100: #C8E6C9;
    -admin-green-050: #E8F5E9;

    /* Neutros */
    -admin-bg-dark:   #1A1A1A;
    -admin-bg-medium: #2D2D2D;
    -admin-bg-light:  #F5F5F5;
    -admin-bg-white:  #FFFFFF;
    -admin-border:    #E0E0E0;
    -admin-text:      #212121;
    -admin-text-muted:#757575;

    /* Feedback */
    -admin-success:   #2E7D32;
    -admin-warning:   #F57F17;
    -admin-danger:    #C62828;
    -admin-info:      #1565C0;

    /* Tipografia */
    -fx-font-family: "Segoe UI", "Liberation Sans", sans-serif;
    -fx-font-size: 13px;
}

/* ── Raiz / Container Principal ────────────────────────────────── */
.admin-root {
    -fx-background-color: -admin-bg-light;
}

/* ── Barra de Título ────────────────────────────────────────────── */
.admin-title-bar {
    -fx-background-color: -admin-green-900;
    -fx-padding: 8 16 8 16;
    -fx-spacing: 12;
    -fx-min-height: 48;
}

.admin-app-name {
    -fx-text-fill: white;
    -fx-font-size: 18px;
    -fx-font-weight: bold;
    -fx-font-family: "Segoe UI Semibold";
}

.admin-module-name {
    -fx-text-fill: -admin-green-300;
    -fx-font-size: 13px;
    -fx-font-style: italic;
}

.admin-empresa-nome {
    -fx-text-fill: white;
    -fx-font-size: 12px;
}

.admin-user-label {
    -fx-text-fill: -admin-green-300;
    -fx-font-size: 12px;
}

.admin-version-label {
    -fx-text-fill: -admin-green-400;
    -fx-font-size: 11px;
}

.admin-sep {
    -fx-background-color: -admin-green-700;
}

/* ── Ribbon / Toolbar ───────────────────────────────────────────── */
.admin-ribbon {
    -fx-background-color: -admin-green-800;
    -fx-padding: 4 8 4 8;
    -fx-spacing: 2;
    -fx-min-height: 64;
}

.admin-ribbon-btn {
    -fx-background-color: transparent;
    -fx-background-radius: 4;
    -fx-border-color: transparent;
    -fx-border-radius: 4;
    -fx-padding: 4 8 4 8;
    -fx-cursor: hand;
    -fx-min-width: 68;
}

.admin-ribbon-btn:hover {
    -fx-background-color: -admin-green-700;
}

.admin-ribbon-btn:pressed {
    -fx-background-color: -admin-green-600;
}

.admin-ribbon-label {
    -fx-text-fill: white;
    -fx-font-size: 11px;
    -fx-alignment: center;
}

.admin-icon-green {
    -fx-icon-color: -admin-green-300;
}

.admin-icon-red {
    -fx-icon-color: #EF9A9A;
}

/* ── TabPane ────────────────────────────────────────────────────── */
.admin-tab-pane > .tab-header-area {
    -fx-background-color: #ECEFF1;
    -fx-padding: 4 4 0 4;
}

.admin-tab-pane > .tab-header-area > .headers-region > .tab {
    -fx-background-color: #CFD8DC;
    -fx-background-radius: 4 4 0 0;
    -fx-padding: 6 14;
    -fx-border-color: #B0BEC5 #B0BEC5 transparent;
    -fx-border-width: 1 1 0;
}

.admin-tab-pane > .tab-header-area > .headers-region > .tab:selected {
    -fx-background-color: white;
    -fx-border-color: -admin-green-500 -admin-green-500 transparent;
    -fx-border-width: 2 1 0;
}

.admin-tab-pane > .tab-header-area > .headers-region > .tab .tab-label {
    -fx-font-size: 12px;
    -fx-text-fill: -admin-text-muted;
}

.admin-tab-pane > .tab-header-area > .headers-region > .tab:selected .tab-label {
    -fx-text-fill: -admin-green-800;
    -fx-font-weight: bold;
}

.admin-tab-icon {
    -fx-icon-color: -admin-green-600;
}

/* ── Cards ──────────────────────────────────────────────────────── */
.admin-card {
    -fx-background-color: white;
    -fx-border-color: -admin-border;
    -fx-border-radius: 6;
    -fx-background-radius: 6;
    -fx-padding: 16;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 2);
}

.admin-card-title {
    -fx-font-size: 14px;
    -fx-font-weight: bold;
    -fx-text-fill: -admin-green-800;
    -fx-border-color: -admin-green-100;
    -fx-border-width: 0 0 1 0;
    -fx-padding: 0 0 8 0;
}

/* ── KPI Cards ──────────────────────────────────────────────────── */
.admin-kpi-card {
    -fx-background-color: white;
    -fx-border-radius: 8;
    -fx-background-radius: 8;
    -fx-padding: 16;
    -fx-border-width: 0 0 0 4;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 6, 0, 0, 3);
    -fx-alignment: center;
}

.admin-kpi-valor {
    -fx-font-size: 28px;
    -fx-font-weight: bold;
    -fx-text-fill: -admin-green-800;
}

.admin-kpi-titulo {
    -fx-font-size: 12px;
    -fx-text-fill: -admin-text-muted;
    -fx-alignment: center;
}

/* ── Formulários ────────────────────────────────────────────────── */
.admin-form-container {
    -fx-padding: 16;
    -fx-spacing: 12;
}

.admin-section-title {
    -fx-font-size: 16px;
    -fx-font-weight: bold;
    -fx-text-fill: -admin-green-800;
}

.admin-section-header {
    -fx-background-color: -admin-green-050;
    -fx-border-color: -admin-green-100;
    -fx-border-width: 0 0 1 0;
    -fx-padding: 8 12;
    -fx-spacing: 8;
    -fx-alignment: CENTER_LEFT;
}

.admin-label {
    -fx-text-fill: #424242;
    -fx-font-size: 12px;
    -fx-font-weight: bold;
}

.admin-field {
    -fx-background-color: white;
    -fx-border-color: #BDBDBD;
    -fx-border-radius: 4;
    -fx-background-radius: 4;
    -fx-padding: 6 8;
    -fx-font-size: 13px;
}

.admin-field:focused {
    -fx-border-color: -admin-green-500;
    -fx-border-width: 1.5;
    -fx-effect: dropshadow(gaussian, rgba(76,175,80,0.3), 4, 0, 0, 0);
}

.admin-field:hover {
    -fx-border-color: -admin-green-400;
}

.admin-numeric {
    -fx-alignment: center-right;
    -fx-font-family: "Consolas", monospace;
}

.admin-combo {
    -fx-background-color: white;
    -fx-border-color: #BDBDBD;
    -fx-border-radius: 4;
}

.admin-combo:focused {
    -fx-border-color: -admin-green-500;
}

.admin-titled-pane > .title {
    -fx-background-color: -admin-green-050;
    -fx-font-weight: bold;
    -fx-text-fill: -admin-green-800;
    -fx-font-size: 12px;
}

.admin-titled-pane > .content {
    -fx-border-color: -admin-green-100;
    -fx-padding: 12;
}

/* ── Botões ─────────────────────────────────────────────────────── */
.admin-btn-primary {
    -fx-background-color: -admin-green-700;
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-background-radius: 4;
    -fx-border-radius: 4;
    -fx-padding: 7 16;
    -fx-cursor: hand;
    -fx-font-size: 12px;
}

.admin-btn-primary:hover {
    -fx-background-color: -admin-green-600;
}

.admin-btn-primary:pressed {
    -fx-background-color: -admin-green-800;
}

.admin-btn-save {
    -fx-background-color: -admin-green-700;
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-background-radius: 4;
    -fx-padding: 7 20;
    -fx-cursor: hand;
}

.admin-btn-save:hover {
    -fx-background-color: -admin-green-600;
}

.admin-btn-secondary {
    -fx-background-color: white;
    -fx-border-color: -admin-green-500;
    -fx-text-fill: -admin-green-700;
    -fx-font-weight: bold;
    -fx-background-radius: 4;
    -fx-border-radius: 4;
    -fx-padding: 6 14;
    -fx-cursor: hand;
}

.admin-btn-secondary:hover {
    -fx-background-color: -admin-green-050;
}

.admin-btn-cancel {
    -fx-background-color: #F5F5F5;
    -fx-border-color: #BDBDBD;
    -fx-text-fill: #424242;
    -fx-background-radius: 4;
    -fx-border-radius: 4;
    -fx-padding: 6 14;
    -fx-cursor: hand;
}

.admin-btn-danger {
    -fx-background-color: -admin-danger;
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-background-radius: 4;
    -fx-border-radius: 4;
    -fx-padding: 7 16;
    -fx-cursor: hand;
}

.admin-btn-danger:hover {
    -fx-background-color: #B71C1C;
}

.admin-btn-danger-outline {
    -fx-background-color: transparent;
    -fx-border-color: -admin-danger;
    -fx-text-fill: -admin-danger;
    -fx-background-radius: 4;
    -fx-border-radius: 4;
    -fx-padding: 6 14;
    -fx-cursor: hand;
}

.admin-btn-success {
    -fx-background-color: -admin-success;
    -fx-text-fill: white;
    -fx-background-radius: 4;
    -fx-border-radius: 4;
    -fx-padding: 7 16;
    -fx-cursor: hand;
}

/* ── Tabelas ────────────────────────────────────────────────────── */
.admin-table {
    -fx-border-color: -admin-border;
    -fx-border-radius: 4;
    -fx-background-radius: 4;
}

.admin-table > .column-header-background {
    -fx-background-color: -admin-green-800;
}

.admin-table .column-header-background .label {
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-font-size: 12px;
}

.admin-table .table-row-cell {
    -fx-cell-size: 32px;
}

.admin-table .table-row-cell:odd {
    -fx-background-color: white;
}

.admin-table .table-row-cell:even {
    -fx-background-color: #F9FBE7;
}

.admin-table .table-row-cell:selected {
    -fx-background-color: -admin-green-100;
}

.admin-table .table-row-cell:hover {
    -fx-background-color: -admin-green-050;
}

/* ── Status Bar ─────────────────────────────────────────────────── */
.admin-status-bar {
    -fx-background-color: -admin-green-900;
    -fx-padding: 4 12;
    -fx-spacing: 8;
    -fx-min-height: 28;
}

.admin-status-text {
    -fx-text-fill: -admin-green-300;
    -fx-font-size: 11px;
}

.admin-status-hint {
    -fx-text-fill: -admin-green-700;
    -fx-font-size: 10px;
}

/* ── Módulos Cards ──────────────────────────────────────────────── */
.admin-modulo-card {
    -fx-background-color: white;
    -fx-border-color: -admin-border;
    -fx-border-radius: 8;
    -fx-background-radius: 8;
    -fx-padding: 16;
    -fx-alignment: center;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);
    -fx-cursor: default;
}

.admin-modulo-card:hover {
    -fx-border-color: -admin-green-400;
    -fx-effect: dropshadow(gaussian, rgba(76,175,80,0.2), 10, 0, 0, 4);
}

.admin-modulo-nome {
    -fx-font-size: 14px;
    -fx-font-weight: bold;
    -fx-text-fill: -admin-text;
    -fx-alignment: center;
}

.admin-modulo-versao {
    -fx-font-size: 11px;
    -fx-text-fill: -admin-text-muted;
}

.admin-modulo-desc {
    -fx-font-size: 11px;
    -fx-text-fill: -admin-text-muted;
    -fx-text-alignment: center;
    -fx-alignment: center;
}

.admin-modulo-badge {
    -fx-font-size: 10px;
    -fx-padding: 2 8;
    -fx-background-radius: 10;
    -fx-font-weight: bold;
}

.badge-success {
    -fx-background-color: -admin-green-100;
    -fx-text-fill: -admin-green-800;
}

.badge-default {
    -fx-background-color: #EEEEEE;
    -fx-text-fill: #757575;
}

/* ── Auditoria ──────────────────────────────────────────────────── */
.admin-filter-pane > .title {
    -fx-background-color: #F3E5F5;
    -fx-text-fill: #6A1B9A;
    -fx-font-weight: bold;
}

/* ── Logótipo ───────────────────────────────────────────────────── */
.admin-logo-area {
    -fx-background-color: #F5F5F5;
    -fx-border-color: -admin-border;
    -fx-border-radius: 6;
    -fx-background-radius: 6;
    -fx-padding: 12;
    -fx-border-style: dashed;
}

/* ── Warning ────────────────────────────────────────────────────── */
.admin-warning-label {
    -fx-text-fill: -admin-warning;
    -fx-font-weight: bold;
    -fx-background-color: #FFF8E1;
    -fx-border-color: #FFE082;
    -fx-border-radius: 4;
    -fx-background-radius: 4;
    -fx-padding: 8;
}

/* ── Toolbar de Listagem ────────────────────────────────────────── */
.admin-toolbar {
    -fx-background-color: white;
    -fx-border-color: -admin-border;
    -fx-border-width: 0 0 1 0;
    -fx-padding: 10 16;
    -fx-spacing: 8;
}

.admin-filter-bar {
    -fx-background-color: -admin-green-050;
    -fx-border-color: -admin-green-100;
    -fx-border-radius: 4;
    -fx-background-radius: 4;
    -fx-padding: 8 12;
    -fx-spacing: 8;
}

/* ── Hint/Placeholder ───────────────────────────────────────────── */
.admin-hint-label {
    -fx-text-fill: -admin-text-muted;
    -fx-font-size: 11px;
    -fx-font-style: italic;
}

.admin-placeholder {
    -fx-text-fill: -admin-text-muted;
    -fx-font-style: italic;
}

/* ── Dashboard Chart ────────────────────────────────────────────── */
.admin-chart {
    -fx-background-color: white;
    -fx-border-color: -admin-border;
    -fx-border-radius: 6;
    -fx-background-radius: 6;
}

/* ── ScrollPane ─────────────────────────────────────────────────── */
.admin-form-scroll {
    -fx-background: -admin-bg-light;
    -fx-background-color: -admin-bg-light;
}
```

---

## 14. Migrações Flyway — Scripts SQL Completos

### 14.1 Script de Migração Base

```sql
-- src/main/resources/db/migration/admin/V1_0_0__create_admin_schema.sql

-- ════════════════════════════════════════════════════════════════════
-- KUBATA ADMINISTRATOR - SCHEMA INICIAL
-- Versão: 1.0.0
-- Data: 2025-01-01
-- ════════════════════════════════════════════════════════════════════

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ── Empresa ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS adm_empresa (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    nif                     VARCHAR(14)  NOT NULL UNIQUE,
    denominacao_social      VARCHAR(200) NOT NULL,
    nome_comercial          VARCHAR(150),
    tipo_contribuinte       ENUM('PESSOA_SINGULAR','PESSOA_COLECTIVA','NAO_RESIDENTE') NOT NULL DEFAULT 'PESSOA_COLECTIVA',
    regime_iva              ENUM('GERAL','SIMPLIFICADO','ISENTO','ESPECIAL') NOT NULL DEFAULT 'GERAL',
    endereco                VARCHAR(300),
    municipio               VARCHAR(100),
    provincia               VARCHAR(100),
    pais                    VARCHAR(3)   NOT NULL DEFAULT 'AO',
    caixa_postal            VARCHAR(20),
    telefone                VARCHAR(20),
    telemovel               VARCHAR(20),
    fax                     VARCHAR(20),
    email                   VARCHAR(100),
    website                 VARCHAR(100),
    iban                    VARCHAR(34),
    banco                   VARCHAR(100),
    conta_bancaria          VARCHAR(30),
    codigo_cae              VARCHAR(10),
    descricao_actividade    VARCHAR(200),
    data_constituicao       DATE,
    conservatoria           VARCHAR(100),
    matricula_comercial     VARCHAR(50),
    capital_social          DOUBLE,
    numero_certificado_agt  VARCHAR(50),
    versao_certificado_agt  VARCHAR(20),
    data_certificado_agt    DATE,
    hash_certificado_agt    VARCHAR(64),
    moeda_base              VARCHAR(3)   NOT NULL DEFAULT 'AOA',
    casas_decimais_valor    INT          NOT NULL DEFAULT 2,
    casas_decimais_quantidade INT        NOT NULL DEFAULT 3,
    exercicio_actual        INT,
    logotipo                LONGBLOB,
    logotipo_mime_type      VARCHAR(50),
    rodape_documento        VARCHAR(500),
    mensagem_fatura         VARCHAR(300),
    criado_em               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em           DATETIME     ON UPDATE CURRENT_TIMESTAMP,
    ativo                   TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT chk_nif CHECK (nif REGEXP '^[0-9]{9,14}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Dados da empresa/contribuinte';

-- ── Perfis de Acesso ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS adm_perfil_acesso (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo      VARCHAR(20)  NOT NULL UNIQUE,
    descricao   VARCHAR(100) NOT NULL,
    observacoes VARCHAR(500),
    sistema     TINYINT(1)   NOT NULL DEFAULT 0,
    activo      TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT uk_perfil_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Permissões por Perfil ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS adm_permissao_perfil (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    perfil_id   BIGINT       NOT NULL,
    modulo      VARCHAR(50)  NOT NULL,
    recurso     VARCHAR(50)  NOT NULL,
    operacao    ENUM('VER','CRIAR','EDITAR','APAGAR','IMPRIMIR','EXPORTAR','APROVAR','ANULAR') NOT NULL,
    permitido   TINYINT(1)   NOT NULL DEFAULT 0,
    CONSTRAINT uk_perm UNIQUE (perfil_id, modulo, recurso, operacao),
    CONSTRAINT fk_perm_perfil FOREIGN KEY (perfil_id)
        REFERENCES adm_perfil_acesso(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Utilizadores ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS adm_utilizador (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username                    VARCHAR(50)  NOT NULL UNIQUE,
    password_hash               VARCHAR(255) NOT NULL,
    password_provisoria         TINYINT(1)   NOT NULL DEFAULT 1,
    data_expiracao_password     DATE,
    nome_completo               VARCHAR(150) NOT NULL,
    email                       VARCHAR(100) UNIQUE,
    telefone                    VARCHAR(20),
    departamento                VARCHAR(100),
    cargo                       VARCHAR(100),
    avatar                      MEDIUMBLOB,
    estado                      ENUM('ACTIVO','INACTIVO','BLOQUEADO','PENDENTE','EXPIRADO')
                                NOT NULL DEFAULT 'ACTIVO',
    tentativas_login_falhas     INT          NOT NULL DEFAULT 0,
    bloqueado_ate               DATETIME,
    ultimo_login                DATETIME,
    ultimo_ip_login             VARCHAR(45),
    superadmin                  TINYINT(1)   NOT NULL DEFAULT 0,
    idioma                      VARCHAR(5)   NOT NULL DEFAULT 'pt-AO',
    tema                        VARCHAR(30)  NOT NULL DEFAULT 'VERDE_ADMIN',
    linhas_por_pagina           INT          NOT NULL DEFAULT 50,
    empresa_id                  BIGINT,
    criado_em                   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em               DATETIME     ON UPDATE CURRENT_TIMESTAMP,
    criado_por                  VARCHAR(50),
    CONSTRAINT fk_user_empresa FOREIGN KEY (empresa_id)
        REFERENCES adm_empresa(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Relação Utilizador ↔ Perfil ──────────────────────────────────
CREATE TABLE IF NOT EXISTS adm_utilizador_perfil (
    utilizador_id   BIGINT NOT NULL,
    perfil_id       BIGINT NOT NULL,
    PRIMARY KEY (utilizador_id, perfil_id),
    CONSTRAINT fk_up_user FOREIGN KEY (utilizador_id)
        REFERENCES adm_utilizador(id) ON DELETE CASCADE,
    CONSTRAINT fk_up_perfil FOREIGN KEY (perfil_id)
        REFERENCES adm_perfil_acesso(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ── Séries de Documentos ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS adm_serie_documento (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id          BIGINT       NOT NULL,
    tipo_documento      ENUM('FT','FR','NC','ND','GD','GT','GR','CC','ORC','ECF','RG','PG','DC','VD') NOT NULL,
    serie               VARCHAR(10)  NOT NULL,
    descricao           VARCHAR(200),
    ultimo_numero       BIGINT       NOT NULL DEFAULT 0,
    numero_inicial      BIGINT       NOT NULL DEFAULT 1,
    prefixo             VARCHAR(10),
    formato_numero      VARCHAR(30)  NOT NULL DEFAULT '{PREFIXO} {SERIE}/{NUMERO}',
    data_inicio         DATE,
    data_fim            DATE,
    exercicio           INT,
    estado              ENUM('ACTIVA','INACTIVA','ENCERRADA','PENDENTE_AGT') NOT NULL DEFAULT 'ACTIVA',
    predefinida         TINYINT(1)   NOT NULL DEFAULT 0,
    registada_agt       TINYINT(1)   NOT NULL DEFAULT 0,
    data_registo_agt    DATETIME,
    codigo_validacao_agt VARCHAR(50),
    criado_em           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_serie UNIQUE (empresa_id, tipo_documento, serie),
    CONSTRAINT fk_serie_empresa FOREIGN KEY (empresa_id)
        REFERENCES adm_empresa(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Exercícios Fiscais ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS adm_exercicio_fiscal (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id      BIGINT       NOT NULL,
    ano             INT          NOT NULL,
    data_inicio     DATE         NOT NULL,
    data_fim        DATE         NOT NULL,
    estado          ENUM('FUTURO','ABERTO','ENCERRAMENTO','FECHADO') NOT NULL DEFAULT 'ABERTO',
    encerrado_em    DATETIME,
    encerrado_por   VARCHAR(50),
    observacoes     VARCHAR(500),
    CONSTRAINT uk_exercicio UNIQUE (empresa_id, ano),
    CONSTRAINT fk_exercicio_empresa FOREIGN KEY (empresa_id)
        REFERENCES adm_empresa(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Moedas ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS adm_moeda (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_iso          VARCHAR(3)   NOT NULL UNIQUE,
    nome                VARCHAR(100) NOT NULL,
    simbolo             VARCHAR(10),
    taxa_cambio         DECIMAL(18,6),
    data_taxa_cambio    DATE,
    moeda_base          TINYINT(1)   NOT NULL DEFAULT 0,
    casas_decimais      INT          NOT NULL DEFAULT 2,
    activa              TINYINT(1)   NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Taxas de IVA ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS adm_taxa_iva (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo          VARCHAR(10)  NOT NULL UNIQUE,
    descricao       VARCHAR(100) NOT NULL,
    percentagem     DECIMAL(5,2) NOT NULL,
    codigo_saft     VARCHAR(10),
    vigente_desde   DATE,
    activa          TINYINT(1)   NOT NULL DEFAULT 1,
    padrao          TINYINT(1)   NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Módulos do Sistema ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS adm_modulo_sistema (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo              VARCHAR(30)  NOT NULL UNIQUE,
    nome                VARCHAR(100) NOT NULL,
    descricao           VARCHAR(500),
    versao              VARCHAR(20),
    versao_minima_core  VARCHAR(20),
    icone_classe        VARCHAR(100),
    cor_hex             VARCHAR(7),
    estado              ENUM('DISPONIVEL','ACTIVO','INACTIVO','ERRO','ACTUALIZACAO_PENDENTE')
                        NOT NULL DEFAULT 'DISPONIVEL',
    obrigatorio         TINYINT(1)   NOT NULL DEFAULT 0,
    instalado_em        DATETIME,
    desactivado_em      DATETIME,
    licenca_chave       VARCHAR(200),
    licenca_validade    DATETIME,
    ordem_menu          INT          NOT NULL DEFAULT 99
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Parâmetros do Sistema ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS adm_parametro_sistema (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id      BIGINT,
    chave           VARCHAR(100) NOT NULL,
    valor           VARCHAR(1000),
    tipo_valor      VARCHAR(20)  NOT NULL DEFAULT 'STRING',
    descricao       VARCHAR(500),
    editavel        TINYINT(1)   NOT NULL DEFAULT 1,
    grupo           VARCHAR(50),
    atualizado_em   DATETIME,
    atualizado_por  VARCHAR(50),
    CONSTRAINT uk_parametro UNIQUE (empresa_id, chave),
    CONSTRAINT fk_param_empresa FOREIGN KEY (empresa_id)
        REFERENCES adm_empresa(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Log de Auditoria ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS adm_log_auditoria (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    utilizador_id   BIGINT,
    username        VARCHAR(50),
    ip_address      VARCHAR(45),
    operacao        ENUM('LOGIN','LOGOUT','LOGIN_FALHOU','CRIAR','EDITAR','APAGAR','CONSULTAR',
                         'IMPRIMIR','EXPORTAR','IMPORTAR','APROVAR','ANULAR','ENCERRAR',
                         'BACKUP','RESTAURO','CONFIGURACAO','INSTALACAO_MODULO') NOT NULL,
    modulo          VARCHAR(50),
    entidade        VARCHAR(100),
    entidade_id     VARCHAR(50),
    descricao       VARCHAR(1000),
    dados_anteriores TEXT,
    dados_novos     TEXT,
    criado_em       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    duracao_ms      BIGINT,
    resultado       ENUM('SUCESSO','FALHA','PARCIAL') NOT NULL DEFAULT 'SUCESSO',
    mensagem_erro   VARCHAR(2000),
    INDEX idx_log_utilizador (utilizador_id),
    INDEX idx_log_entidade (entidade, entidade_id),
    INDEX idx_log_data (criado_em),
    INDEX idx_log_operacao (operacao),
    INDEX idx_log_modulo (modulo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 14.2 Script de Dados Iniciais

```sql
-- src/main/resources/db/migration/admin/V1_0_1__insert_dados_iniciais.sql

-- ════════════════════════════════════════════════════════════════════
-- DADOS INICIAIS — KUBATA ADMINISTRATOR
-- ════════════════════════════════════════════════════════════════════

-- ── Perfis Padrão do Sistema ──────────────────────────────────────
INSERT INTO adm_perfil_acesso (codigo, descricao, sistema, activo) VALUES
('SUPERADMIN', 'Super Administrador — Acesso Total', 1, 1),
('ADMIN',      'Administrador do Sistema',           1, 1),
('FATURADOR',  'Operador de Faturação',              1, 1),
('CONTABILISTA','Contabilista',                      1, 1),
('CONSULTOR',  'Consultor (Apenas Leitura)',          1, 1),
('GESTOR',     'Gestor Geral',                       1, 1);

-- Permissões do SUPERADMIN (todas)
INSERT INTO adm_permissao_perfil (perfil_id, modulo, recurso, operacao, permitido)
SELECT 1, m.modulo, r.recurso, o.operacao, 1
FROM (SELECT 'ADMINISTRATOR' modulo UNION SELECT 'FATURACAO'
      UNION SELECT 'CLIENTES' UNION SELECT 'STOCK' UNION SELECT 'RH'
      UNION SELECT 'RELATORIOS' UNION SELECT 'FINANCEIRO') m,
     (SELECT 'TODOS' recurso) r,
     (SELECT 'VER' operacao UNION SELECT 'CRIAR' UNION SELECT 'EDITAR'
      UNION SELECT 'APAGAR' UNION SELECT 'IMPRIMIR' UNION SELECT 'EXPORTAR'
      UNION SELECT 'APROVAR' UNION SELECT 'ANULAR') o;

-- ── Utilizador Administrador Padrão ──────────────────────────────
-- Password: Admin@123 (BCrypt — ALTERAR NO PRIMEIRO LOGIN!)
INSERT INTO adm_utilizador
    (username, password_hash, password_provisoria, nome_completo,
     email, estado, superadmin, idioma, tema)
VALUES
    ('admin',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LPVyX5Bnv3e',
     1, 'Administrador do Sistema',
     'admin@kubata.ao', 'ACTIVO', 1, 'pt-AO', 'VERDE_ADMIN');

-- Associar ao perfil SUPERADMIN
INSERT INTO adm_utilizador_perfil (utilizador_id, perfil_id) VALUES (1, 1);

-- ── Moedas ───────────────────────────────────────────────────────
INSERT INTO adm_moeda (codigo_iso, nome, simbolo, taxa_cambio, moeda_base, casas_decimais, activa)
VALUES
('AOA', 'Kwanza Angolano',     'Kz', 1.000000,      1, 2, 1),
('USD', 'Dólar Americano',     '$',  840.000000,     0, 2, 1),
('EUR', 'Euro',                '€',  920.000000,     0, 2, 1),
('GBP', 'Libra Esterlina',     '£',  1050.000000,    0, 2, 1),
('ZAR', 'Rand Sul-Africano',   'R',  45.000000,      0, 2, 1),
('CNY', 'Yuan Chinês',         '¥',  116.000000,     0, 2, 0);

-- ── Taxas de IVA Angolanas ────────────────────────────────────────
INSERT INTO adm_taxa_iva (codigo, descricao, percentagem, codigo_saft, vigente_desde, padrao)
VALUES
('IVA14', 'IVA Taxa Normal — 14%',       14.00, 'NOR', '2019-10-01', 1),
('ISE',   'Isento de IVA',                0.00, 'ISE', '2019-10-01', 0),
('RED7',  'IVA Taxa Reduzida — 7%',       7.00, 'RED', '2019-10-01', 0),
('OUT',   'Fora do Âmbito do IVA',        0.00, 'OUT', '2019-10-01', 0);

-- ── Módulos do Sistema ───────────────────────────────────────────
INSERT INTO adm_modulo_sistema (codigo, nome, descricao, versao, icone_classe, cor_hex, estado, obrigatorio, ordem_menu)
VALUES
('ADMINISTRATOR', 'Administrator',    'Gestão do sistema, utilizadores e configurações', '1.0.0', 'mdi2c-cog',                 '#1B5E20', 'ACTIVO',    1, 1),
('FATURACAO',     'Faturação',        'Emissão de faturas, notas de crédito e débito',  '1.0.0', 'mdi2c-cash-register',       '#1565C0', 'ACTIVO',    0, 2),
('CLIENTES',      'Clientes',         'Gestão de clientes, fornecedores e contactos',   '1.0.0', 'mdi2a-account-group',       '#6A1B9A', 'ACTIVO',    0, 3),
('STOCK',         'Stock',            'Gestão de artigos, stock e movimentos',          '1.0.0', 'mdi2w-warehouse',            '#E65100', 'DISPONIVEL',0, 4),
('RH',            'Recursos Humanos', 'Gestão de funcionários, folha salarial',         '1.0.0', 'mdi2a-account-hard-hat',    '#4E342E', 'DISPONIVEL',0, 5),
('CONTABILIDADE', 'Contabilidade',    'Plano de contas, lançamentos e balancetes',      '1.0.0', 'mdi2b-book-open-page-variant','#263238','DISPONIVEL',0, 6),
('RELATORIOS',    'Relatórios',       'Relatórios gerenciais e fiscais',                '1.0.0', 'mdi2c-chart-bar',           '#00695C', 'ACTIVO',    0, 7),
('POS',           'Ponto de Venda',   'Terminal de venda rápida',                       '1.0.0', 'mdi2c-cash-multiple',       '#F57F17', 'DISPONIVEL',0, 8);

-- ── Parâmetros Globais do Sistema ────────────────────────────────
INSERT INTO adm_parametro_sistema (empresa_id, chave, valor, tipo_valor, descricao, grupo)
VALUES
(NULL, 'SISTEMA.NOME_APP',          'Kubata Faturação',  'STRING',  'Nome da aplicação', 'SISTEMA'),
(NULL, 'SISTEMA.VERSAO',            '1.0.0',             'STRING',  'Versão actual', 'SISTEMA'),
(NULL, 'UI.TEMA',                   'VERDE_ADMIN',        'STRING',  'Tema da interface', 'INTERFACE'),
(NULL, 'UI.IDIOMA',                 'pt-AO',              'STRING',  'Idioma da interface', 'INTERFACE'),
(NULL, 'UI.LINHAS_PAGINA',          '50',                 'INTEGER', 'Linhas por página nas tabelas', 'INTERFACE'),
(NULL, 'SEGURANCA.MAX_TENTATIVAS',  '5',                  'INTEGER', 'Tentativas de login antes de bloqueio', 'SEGURANCA'),
(NULL, 'SEGURANCA.MINUTOS_BLOQUEIO','30',                 'INTEGER', 'Minutos de bloqueio após tentativas falhadas', 'SEGURANCA'),
(NULL, 'SEGURANCA.DIAS_VALIDADE_PW','90',                 'INTEGER', 'Dias de validade da password', 'SEGURANCA'),
(NULL, 'BACKUP.RETENCAO_DIAS',      '30',                 'INTEGER', 'Dias de retenção dos backups', 'BACKUP'),
(NULL, 'BACKUP.AUTO_ACTIVO',        'true',               'BOOLEAN', 'Backup automático activo', 'BACKUP'),
(NULL, 'BACKUP.DIRETORIO',          'backups/',           'STRING',  'Diretório dos backups', 'BACKUP');
```

---

## 15. Guia de Instalação Step-by-Step

### 15.1 Pré-requisitos

| Componente | Versão Mínima | Download |
|---|---|---|
| **JDK** | 21 LTS | adoptium.net |
| **Maven** | 3.9+ | maven.apache.org |
| **MySQL** | 8.0+ | mysql.com |
| **Git** | 2.40+ | git-scm.com |
| **IDE** | IntelliJ IDEA 2024+ | jetbrains.com |
| **JavaFX** | 21 SDK | gluonhq.com/products/javafx |

### 15.2 Passo 1 — Clonar e Configurar o Projecto

```bash
# Clonar o repositório
git clone https://github.com/seuusuario/kubata.git
cd kubata

# Verificar a estrutura
ls -la

# Verificar que todos os módulos existem
ls kubata-administrator/
```

### 15.3 Passo 2 — Configurar a Base de Dados MySQL

```sql
-- Executar no MySQL como root
CREATE DATABASE IF NOT EXISTS kubata_admin
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'kubata_user'@'localhost'
    IDENTIFIED BY 'Kubata@2025!';

GRANT ALL PRIVILEGES ON kubata_admin.*
    TO 'kubata_user'@'localhost';

FLUSH PRIVILEGES;
```

### 15.4 Passo 3 — Configurar `application.properties`

```properties
# src/main/resources/application.properties (do módulo administrator)

# ── Base de Dados ─────────────────────────────────────────────────
spring.datasource.url=jdbc:mysql://localhost:3306/kubata_admin?useSSL=false&serverTimezone=Africa/Luanda&characterEncoding=UTF-8
spring.datasource.username=kubata_user
spring.datasource.password=Kubata@2025!
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# ── JPA / Hibernate ───────────────────────────────────────────────
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false

# ── Flyway ────────────────────────────────────────────────────────
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration/admin
spring.flyway.baseline-on-migrate=true
spring.flyway.out-of-order=false

# ── Kubata Específico ─────────────────────────────────────────────
kubata.backup.diretorio=C:/KubataBackups
kubata.admin.max-tentativas-login=5
kubata.admin.minutos-bloqueio=30

# ── JavaFX ────────────────────────────────────────────────────────
javafx.stage.title=Kubata Administrator
javafx.stage.width=1280
javafx.stage.height=800

# ── Logging ───────────────────────────────────────────────────────
logging.level.ao.kubata=INFO
logging.level.org.springframework=WARN
logging.file.name=logs/kubata-admin.log
logging.logback.rollingpolicy.max-file-size=10MB
logging.logback.rollingpolicy.max-history=30
```

### 15.5 Passo 4 — Compilar o Projecto

```bash
# Na raiz do projecto, compilar todos os módulos
mvn clean install -DskipTests

# Verificar que não há erros de compilação
echo "Exit code: $?"

# Executar apenas o módulo administrator
mvn -pl kubata-administrator spring-boot:run

# OU executar o JAR gerado
java --module-path /path/to/javafx-sdk-21/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar kubata-administrator/target/kubata-administrator-1.0.0.jar
```

### 15.6 Passo 5 — Primeiro Login

1. Iniciar a aplicação
2. No ecrã de login, usar:
   - **Utilizador:** `admin`
   - **Password:** `Admin@123`
3. O sistema pedirá para alterar a password provisória
4. Definir nova password com critérios:
   - Mínimo 8 caracteres
   - Pelo menos 1 maiúscula
   - Pelo menos 1 número
   - Pelo menos 1 caracter especial

### 15.7 Passo 6 — Configurar a Empresa

1. Navegar a **Administrator → Empresa**
2. Preencher obrigatoriamente:
   - NIF da empresa
   - Denominação Social
   - Tipo de Contribuinte
   - Regime de IVA
3. Preencher opcionalmente:
   - Logótipo (PNG/JPG, máx. 2MB)
   - Endereço completo
   - Dados AGT/Certificado
4. Clicar **Guardar**

### 15.8 Passo 7 — Inicializar Séries de Documentos

1. Navegar a **Administrator → Séries de Documentos**
2. Clicar **Inicializar Séries** (cria automaticamente a Série A para todos os tipos)
3. Verificar que as séries foram criadas correctamente
4. Definir a série predefinida para cada tipo de documento

### 15.9 Passo 8 — Criar Utilizadores

```
Administrator → Utilizadores → + Novo Utilizador
```

1. Preencher dados do utilizador
2. Definir password provisória
3. Atribuir perfil de acesso
4. Guardar e comunicar as credenciais ao utilizador

### 15.10 Passo 9 — Activar Módulos

1. Navegar a **Administrator → Módulos**
2. Activar os módulos necessários clicando em **▶ Activar**
3. O sistema validará as dependências antes de activar

### 15.11 Resolução de Problemas Comuns

**Erro: `Flyway migration failed — Duplicate column`**
```bash
# Verificar se existe versão anterior da migração
SELECT version, description, success
FROM kubata_admin.flyway_schema_history
ORDER BY installed_on DESC LIMIT 10;

# Se houver migração com sucesso=0, corrigir e fazer repair
mvn flyway:repair -pl kubata-administrator
```

**Erro: `ClassCastException em TableView`**
```java
// Verificar que o CellValueFactory está correcto
colNome.setCellValueFactory(new PropertyValueFactory<>("nomeCompleto"));
// OU (preferred):
colNome.setCellValueFactory(data -> 
    new SimpleStringProperty(data.getValue().getNomeCompleto()));
```

**Erro: `Ribbon node reparenting`**
```java
// No RibbonController, verificar que os nós do overflow
// são removidos do pai antes de serem adicionados ao popup
Node node = ribbon.getButton();
if (node.getParent() != null) {
    ((Pane) node.getParent()).getChildren().remove(node);
}
popup.getContent().add(node);
```

---

## 16. Exercícios Práticos

### ✅ EXERCÍCIO 1 — Criar um Novo Utilizador com Perfil Personalizado

**Objectivo:** Praticar a criação de utilizadores e gestão de perfis.

**Passos:**
1. Criar um novo perfil chamado `OPERADOR_CAIXA` com permissões:
   - Faturação: VER, CRIAR, IMPRIMIR
   - Clientes: VER
2. Criar utilizador `joao.silva` com password provisória `Caixa@2025`
3. Atribuir o perfil `OPERADOR_CAIXA` ao utilizador
4. Verificar no Log de Auditoria se as operações foram registadas
5. Fazer login como `joao.silva` e verificar que só vê os módulos permitidos

**Validação:**
```sql
SELECT u.username, p.codigo, pp.modulo, pp.recurso, pp.operacao
FROM adm_utilizador u
JOIN adm_utilizador_perfil up ON u.id = up.utilizador_id
JOIN adm_perfil_acesso p ON up.perfil_id = p.id
JOIN adm_permissao_perfil pp ON p.id = pp.perfil_id
WHERE u.username = 'joao.silva' AND pp.permitido = 1;
```

---

### ✅ EXERCÍCIO 2 — Configurar Séries para um Novo Exercício Fiscal

**Objectivo:** Dominar a gestão de séries de documentos SAF-T.

**Passos:**
1. Criar o Exercício Fiscal 2025 (01/01/2025 a 31/12/2025)
2. Criar série `B` para Faturas (FT), com formato `FT B/{NUMERO}`
3. Definir a nova série como predefinida
4. Encerrar a série `A` do exercício anterior
5. Emitir uma fatura de teste e verificar a numeração

**Validação:**
```sql
SELECT tipo_documento, serie, ultimo_numero, estado, predefinida, exercicio
FROM adm_serie_documento
WHERE estado IN ('ACTIVA', 'ENCERRADA')
ORDER BY tipo_documento, exercicio;
```

---

### ✅ EXERCÍCIO 3 — Efectuar e Restaurar um Backup

**Objectivo:** Garantir continuidade de negócio com backups regulares.

**Passos:**
1. Configurar o diretório de backup para `C:\KubataBackups\`
2. Executar um backup manual
3. Verificar que o ficheiro ZIP foi criado
4. Simular um problema: inserir dados de teste e depois apagá-los
5. Restaurar o backup e verificar que os dados voltaram

**Nota:** O restauro requer confirmação explícita e nunca deve ser feito em produção sem aprovação superior.

---

### ✅ EXERCÍCIO 4 — Configurar Parâmetros Fiscais

**Objectivo:** Dominar os parâmetros AGT e SAF-T.

**Passos:**
1. Navegar a **Parâmetros do Sistema**
2. Confirmar que `FISCAL.IVA_PADRAO` está a `14.00`
3. Confirmar `DOC.DIAS_VENCIMENTO` está a `30`
4. Alterar o prazo de vencimento padrão para `45` dias
5. Verificar que o log de auditoria registou a alteração

---

### ✅ EXERCÍCIO 5 — Análise do Log de Auditoria

**Objectivo:** Usar o Log de Auditoria para monitorizar o sistema.

**Passos:**
1. Filtrar os logs do utilizador `admin` do último mês
2. Identificar quantas operações CRIAR foram realizadas
3. Verificar se houve tentativas de login falhadas
4. Exportar os logs para CSV
5. Fazer uma consulta SQL directa para encontrar operações suspeitas:

```sql
-- Tentativas de login falhadas nas últimas 24h
SELECT username, ip_address, COUNT(*) as tentativas, MAX(criado_em) as ultima_tentativa
FROM adm_log_auditoria
WHERE operacao = 'LOGIN_FALHOU'
  AND criado_em >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
GROUP BY username, ip_address
HAVING tentativas > 3
ORDER BY tentativas DESC;
```

---

### ✅ EXERCÍCIO 6 — Implementar uma Nova Permissão no Código

**Objectivo:** Entender o sistema de permissões programaticamente.

**Tarefa:** Adicionar verificação de permissão ao botão "Apagar Cliente" na view de clientes.

```java
// Passo 1: Na view, verificar permissão antes de mostrar o botão
@FXML
public void initialize(URL url, ResourceBundle rb) {
    // Esconder botão de apagar se sem permissão
    btnApagar.setVisible(
        permissaoService.temPermissao(
            sessaoActual.getUtilizador(),
            "CLIENTES",
            "CLIENTES",
            PermissaoPerfil.Operacao.APAGAR
        )
    );
}

// Passo 2: No serviço, proteger o método
@RequerePermissao(modulo = "CLIENTES", recurso = "CLIENTES", operacao = Operacao.APAGAR)
public void apagarCliente(Long clienteId) {
    // lógica de apagar...
}
```

---

### ✅ EXERCÍCIO 7 — Criar CSS para um Novo Estado de Utilizador

**Objectivo:** Personalizar a interface consoante os estados.

**Tarefa:** Na tabela de utilizadores, adicionar código de cores nos badges de estado:

```css
/* Adicionar ao admin-theme.css */
.badge-activo    { -fx-background-color: #C8E6C9; -fx-text-fill: #1B5E20; }
.badge-bloqueado { -fx-background-color: #FFCDD2; -fx-text-fill: #B71C1C; }
.badge-inactivo  { -fx-background-color: #EEEEEE; -fx-text-fill: #757575; }
.badge-pendente  { -fx-background-color: #FFF9C4; -fx-text-fill: #F57F17; }
.badge-expirado  { -fx-background-color: #FFE0B2; -fx-text-fill: #E65100; }
```

```java
// No controller da tabela de utilizadores
colEstado.setCellFactory(col -> new TableCell<Utilizador, Utilizador.EstadoUtilizador>() {
    @Override
    protected void updateItem(Utilizador.EstadoUtilizador estado, boolean empty) {
        super.updateItem(estado, empty);
        if (empty || estado == null) {
            setGraphic(null);
        } else {
            Label badge = new Label(estado.getDescricao());
            badge.getStyleClass().addAll("badge", "badge-" + estado.name().toLowerCase());
            setGraphic(badge);
        }
    }
});
```

---

## 17. Checklist de Homologação

### 17.0 Árvore do ribbon (catálogo declarativo) e mapa recurso → permissão

O Administrator monta o ribbon a partir de `admin/src/main/resources/ao/allon/kubata/admin/ribbon/ribbon-catalog.json` (serviço `RibbonCatalogService` / `RibbonTabLauncher`). Isto permite evoluir o menu sem recompilar a árvore completa em Java, em linha com a escalabilidade do Primavera Administrator.

**Estilos e carga de folhas:** Os estilos `.ribbon-*` residem em `admin/src/main/resources/ao/allon/kubata/admin/ui/styles/ribbon-modern.css`. O `ThemeManager.applyTheme` adiciona primeiro `admin.css` e em seguida `ribbon-modern.css`, para o ribbon herdar a paleta definida em `.root` e aplicar só overrides do ribbon. As janelas flutuantes (`RibbonFloatingWindow`) copiam `scene.getStylesheets()` da cena principal, pelo que o mesmo ficheiro cobre o modo flutuante.

**Animações e acessibilidade:** O ribbon evita animações quando a JVM arranca com `-Dkubata.reducedMotion=true`.

**Homologação manual sugerida (ribbon):** redimensionar a janela para activar o overflow (»); trocar de abas; usar arrastar ou menu de contexto na aba para flutuar e voltar a ancorar; usar o botão na barra verde para minimizar ou expandir o ribbon.

A aba **Infraestrutura** está marcada com `requirePlatformAdmin: true` no JSON: só utilizadores com papel de administrador de plataforma (p.ex. perfil técnico `ADMIN` / super-admin) devem ver esses botões, além de cumprirem `INFRAESTRUTURA` + operação quando aplicável.

| Aba | Grupo | Botões | Recurso `ADMINISTRATOR` | Operação típica |
|-----|--------|--------|-------------------------|------------------|
| Plataforma | Início | Consola, Aplicação, Parâmetros | — / `APLICACAO` / `PARAMETROS` | `VER` |
| Plataforma | Operações | Atalhos novo utilizador, empresa, exercício, série | `UTILIZADORES`, `EMPRESAS`, `EXERCICIOS`, `SERIES` | `CRIAR` |
| Organização | Dados mestre | Empresa, Exercícios | `EMPRESAS`, `EXERCICIOS` | `VER` |
| Segurança | Acesso | Utilizadores, Perfis, Licenças | `UTILIZADORES`, `PERFIS`, `LICENCAS` | `VER` |
| Fiscal | Documentos | Séries, AGT e SAFT | `SERIES`, `FISCAL_AGT` | `VER` |
| Integração | API e Webhooks | API e Webhooks | `INTEGRACOES` | `VER` |
| Manutenção | Sistema | Auditoria, Backup, Relatórios, Monitor | `AUDITORIA`, `BACKUP`, `RELATORIOS`, `MONITOR` | `VER` |
| Infraestrutura | Dados e serviços | Outras BDs, Servidor BD, Planos, Instâncias | `INFRAESTRUTURA` | `VER` / `EDITAR` |

As permissões granulares para o perfil `ADMIN` são semeadas na migração SQLite `V12__Administrator_platform_extensions.sql`. No serviço de segurança do core, o recurso **`TODOS`** continua a funcionar como coringa (qualquer recurso do módulo) para perfis que já usavam essa convenção.

**Novas áreas de dados (homologação):** parâmetros extra de integração e AGT podem ser adicionados por `V14__Admin_Param_Extras.sql`; conexões auxiliares a outras bases residem em `adm_conexao_auxiliar` (`V13__Create_Adm_Conexao_Auxiliar.sql`), utilizadas pelo ecrã «Outras BDs».

### 17.1 Antes do Go-Live

```
ADMINISTRATOR — RIBBON E PARIDADE PRIMAVERA
─────────────────────────────────────────────────────
[ ] Ficheiro ribbon-catalog.json presente e abas visíveis conforme permissões
[ ] Utilizador sem INFRAESTRUTURA não vê a aba Infraestrutura (ou grupos reservados)
[ ] Parâmetros do sistema: leitura/edição global e por empresa; gravação gera auditoria
[ ] Licenciamento: módulos em adm_modulo_sistema com validade e alertas na UI
[ ] Integrações: parâmetros API/Webhooks e ecrã acessível só com INTEGRACOES
[ ] Fiscal AGT/SAFT: ecrã e parâmetros alinhados ao mercado Angola
[ ] Monitor técnico: visível apenas com MONITOR
[ ] Outras BDs: registo e teste de ligação onde implementado (adm_conexao_auxiliar)

CONFIGURAÇÃO BÁSICA
─────────────────────────────────────────────────────
[ ] Empresa configurada com NIF correcto
[ ] Logótipo da empresa carregado (PNG, fundo transparente)
[ ] Regime de IVA definido (Geral — 14%)
[ ] Exercício fiscal criado para o ano corrente
[ ] Séries de documentos criadas e predefinidas
[ ] Todas as séries testadas com documento de teste

UTILIZADORES E SEGURANÇA
─────────────────────────────────────────────────────
[ ] Password do admin padrão alterada
[ ] Utilizadores reais criados (sem usar 'admin' para uso diário)
[ ] Perfis de acesso definidos e testados
[ ] Verificado que FATURADOR não vê módulo RH
[ ] Verificado que CONSULTOR não cria documentos
[ ] Política de password configurada

MÓDULOS
─────────────────────────────────────────────────────
[ ] Módulos necessários activados
[ ] Módulos desnecessários desactivados
[ ] Licenças verificadas (se aplicável)

BACKUP
─────────────────────────────────────────────────────
[ ] Diretório de backup configurado
[ ] Backup automático activado
[ ] Primeiro backup manual realizado e verificado
[ ] Procedimento de restauro testado em ambiente de teste

FISCAL / AGT
─────────────────────────────────────────────────────
[ ] Taxa de IVA padrão: 14%
[ ] Séries de documentos comunicadas à AGT (se exigido)
[ ] Formato de numeração conforme SAF-T AO
[ ] Hash SHA-256 configurado para documentos

AUDITORIA
─────────────────────────────────────────────────────
[ ] Log de auditoria activado
[ ] Retenção de logs configurada (mínimo 5 anos — AGT)
[ ] Acesso ao log restrito a perfil ADMIN

PERFORMANCE
─────────────────────────────────────────────────────
[ ] Índices da base de dados verificados
[ ] Queries lentas identificadas (slow query log)
[ ] Conexões da pool configuradas adequadamente
```

### 17.2 Verificação Final por SQL

```sql
-- Verificar configuração da empresa
SELECT nif, denominacao_social, regime_iva, exercicio_actual
FROM adm_empresa WHERE ativo = 1;

-- Verificar séries activas predefinidas
SELECT tipo_documento, serie, ultimo_numero, estado
FROM adm_serie_documento
WHERE predefinida = 1 AND estado = 'ACTIVA'
ORDER BY tipo_documento;

-- Verificar utilizadores activos
SELECT username, nome_completo, estado,
       GROUP_CONCAT(p.codigo) as perfis
FROM adm_utilizador u
JOIN adm_utilizador_perfil up ON u.id = up.utilizador_id
JOIN adm_perfil_acesso p ON up.perfil_id = p.id
WHERE u.estado = 'ACTIVO'
GROUP BY u.id;

-- Verificar módulos activos
SELECT codigo, nome, versao, estado, instalado_em
FROM adm_modulo_sistema
WHERE estado = 'ACTIVO'
ORDER BY ordem_menu;

-- Verificar parâmetros fiscais críticos
SELECT chave, valor, descricao
FROM adm_parametro_sistema
WHERE grupo = 'FISCAL'
ORDER BY chave;

-- Contar logs de auditoria (deve ser > 0 após usar o sistema)
SELECT operacao, COUNT(*) total, MAX(criado_em) ultima
FROM adm_log_auditoria
GROUP BY operacao
ORDER BY total DESC;

-- Conexões auxiliares (Outras BDs), se a migração V13 tiver corrido
SELECT nome, jdbc_url, username, activo, driver_class, criado_em
FROM adm_conexao_auxiliar
ORDER BY nome;
```

---

## 📚 REFERÊNCIAS E DOCUMENTAÇÃO

| Recurso | URL |
|---|---|
| **Decreto Presidencial nº 320/20** (IVA Angola) | agt.minfin.gov.ao |
| **SAF-T AO — Especificação Técnica** | agt.minfin.gov.ao |
| **AtlantaFX Docs** | mkpaz.github.io/atlantafx |
| **Spring Boot 3.x Reference** | docs.spring.io |
| **JavaFX 21 API** | openjfx.io |
| **Ikonli Icons** | kordamp.org/ikonli |
| **Flyway Docs** | documentation.red-gate.com/fd |
| **ControlsFX** | controlsfx.org |

---

## 🎯 PRÓXIMOS PASSOS SUGERIDOS

Após implementar o módulo Administrator, os próximos módulos a desenvolver por ordem de prioridade são:

1. **Módulo Clientes** — CRM básico (estende o que já existe na ClientesView)
2. **Módulo Stock/Artigos** — Catálogo de produtos com PVP e custo
3. **Módulo Faturação** — Emissão de FT, FR, NC conforme SAF-T AO
4. **Módulo Relatórios** — SAF-T AO, Extracto de Conta, Mapas AGT
5. **Módulo Financeiro** — Recibos, pagamentos, reconciliação

---

*© 2026 Kubata Software, Luanda, Angola — Guia desenvolvido para o Sistema Kubata Faturação v1.0*  
*Este documento é confidencial e de uso interno. Não distribuir sem autorização.*
