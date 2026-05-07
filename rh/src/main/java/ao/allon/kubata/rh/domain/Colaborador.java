package ao.allon.kubata.rh.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "rh_colaboradores",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_colaborador_bi", columnNames = {"bi"}),
           @UniqueConstraint(name = "uk_colaborador_nif", columnNames = {"nif"}),
           @UniqueConstraint(name = "uk_colaborador_mecanografico", columnNames = {"numero_mecanografico"})
       })
public class Colaborador extends BaseEntity {

    @NotBlank(message = "Nome completo é obrigatório")
    @Size(max = 200, message = "Nome não pode exceder 200 caracteres")
    @Column(name = "nome_completo", nullable = false, length = 200)
    private String nomeCompleto;

    @Column(name = "sexo", length = 1)
    private String sexo; // M, F, O

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "estado_civil", length = 20)
    private String estadoCivil; // Solteiro, Casado, Divorciado, Viúvo

    // Identificação
    @Size(max = 14, message = "BI não pode exceder 14 caracteres")
    @Column(name = "bi", length = 14)
    private String bi;

    @Column(name = "bi_validade")
    private LocalDate biValidade;

    @Size(max = 20, message = "NIF não pode exceder 20 caracteres")
    @Column(name = "nif", length = 20)
    private String nif;

    // Contacto
    @Size(max = 20, message = "Telefone não pode exceder 20 caracteres")
    @Column(name = "telefone", length = 20)
    private String telefone;

    @Email(message = "Email inválido")
    @Size(max = 150, message = "Email não pode exceder 150 caracteres")
    @Column(name = "email", length = 150)
    private String email;

    // Morada
    @Size(max = 200, message = "Endereço não pode exceder 200 caracteres")
    @Column(name = "endereco", length = 200)
    private String endereco;

    @Size(max = 100, message = "Cidade não pode exceder 100 caracteres")
    @Column(name = "cidade", length = 100)
    private String cidade;

    @Size(max = 20, message = "Código postal não pode exceder 20 caracteres")
    @Column(name = "codigo_postal", length = 20)
    private String codigoPostal;

    // Dados bancários
    @Size(max = 100, message = "Banco não pode exceder 100 caracteres")
    @Column(name = "banco", length = 100)
    private String banco;

    @Size(max = 34, message = "IBAN não pode exceder 34 caracteres")
    @Column(name = "iban", length = 34)
    private String iban;

    @Size(max = 100, message = "Titular da conta não pode exceder 100 caracteres")
    @Column(name = "conta_titular", length = 100)
    private String contaTitular;

    // Vínculo interno
    @Column(name = "numero_mecanografico", length = 20)
    private String numeroMecanografico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id")
    private Departamento departamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargo_id")
    private Cargo cargo;

    @Column(name = "salario_base", precision = 19, scale = 2)
    private java.math.BigDecimal salarioBase;

    @Column(name = "data_admissao", nullable = false)
    private LocalDate dataAdmissao;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoColaborador estado = EstadoColaborador.ATIVO;

    // Campos de auditoria
    @Column(name = "data_desligamento")
    private LocalDate dataDesligamento;

    @Size(max = 500, message = "Motivo do desligamento não pode exceder 500 caracteres")
    @Column(name = "motivo_desligamento", length = 500)
    private String motivoDesligamento;

    public enum EstadoColaborador {
        ATIVO,
        INATIVO,
        SUSPENSO,
        DESLIGADO
    }

    public Colaborador() {}

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getEstadoCivil() {
        return estadoCivil;
    }

    public void setEstadoCivil(String estadoCivil) {
        this.estadoCivil = estadoCivil;
    }

    public String getBi() {
        return bi;
    }

    public void setBi(String bi) {
        this.bi = bi;
    }

    public LocalDate getBiValidade() {
        return biValidade;
    }

    public void setBiValidade(LocalDate biValidade) {
        this.biValidade = biValidade;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getContaTitular() {
        return contaTitular;
    }

    public void setContaTitular(String contaTitular) {
        this.contaTitular = contaTitular;
    }

    public String getNumeroMecanografico() {
        return numeroMecanografico;
    }

    public void setNumeroMecanografico(String numeroMecanografico) {
        this.numeroMecanografico = numeroMecanografico;
    }

    public Departamento getDepartamento() {
        return departamento;
    }

    public void setDepartamento(Departamento departamento) {
        this.departamento = departamento;
    }

    public Cargo getCargo() {
        return cargo;
    }

    public void setCargo(Cargo cargo) {
        this.cargo = cargo;
    }

    public java.math.BigDecimal getSalarioBase() {
        return salarioBase;
    }

    public void setSalarioBase(java.math.BigDecimal salarioBase) {
        this.salarioBase = salarioBase;
    }

    public LocalDate getDataAdmissao() {
        return dataAdmissao;
    }

    public void setDataAdmissao(LocalDate dataAdmissao) {
        this.dataAdmissao = dataAdmissao;
    }

    public EstadoColaborador getEstado() {
        return estado;
    }

    public void setEstado(EstadoColaborador estado) {
        this.estado = estado;
    }

    public LocalDate getDataDesligamento() {
        return dataDesligamento;
    }

    public void setDataDesligamento(LocalDate dataDesligamento) {
        this.dataDesligamento = dataDesligamento;
    }

    public String getMotivoDesligamento() {
        return motivoDesligamento;
    }

    public void setMotivoDesligamento(String motivoDesligamento) {
        this.motivoDesligamento = motivoDesligamento;
    }

    @Override
    public String toString() {
        return nomeCompleto;
    }
}
