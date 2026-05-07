package ao.allon.kubata.faturacao.domain;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "empresa")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String nif;

    private String endereco;
    private String cidade;
    private String email;
    private String telefone;
    
    @Column(name = "regime_iva")
    private String regimeIva;

    private String website;
    private String slogan;
    private String conservatoria;
    private String capitalSocial;

    @Column(name = "software_validation_number")
    private String softwareValidationNumber;
    
    // Dados Bancários 1
    private String banco1;
    private String iban1;
    
    // Dados Bancários 2
    private String banco2;
    private String iban2;
    
    // Removido @Lob para compatibilidade com SQLite
    // Aumentado para 2MB para garantir suporte a logos de alta resolução
    @Column(length = 2097152)
    private byte[] logotipo;

    public Empresa() {
    }

    public Empresa(String nome, String nif, String endereco, String cidade, String email, String telefone, String regimeIva) {
        this.nome = nome;
        this.nif = nif;
        this.endereco = endereco;
        this.cidade = cidade;
        this.email = email;
        this.telefone = telefone;
        this.regimeIva = regimeIva;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getRegimeIva() {
        return regimeIva;
    }

    public void setRegimeIva(String regimeIva) {
        this.regimeIva = regimeIva;
    }

    public byte[] getLogotipo() {
        return logotipo;
    }

    public void setLogotipo(byte[] logotipo) {
        this.logotipo = logotipo;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getSlogan() {
        return slogan;
    }

    public void setSlogan(String slogan) {
        this.slogan = slogan;
    }

    public String getConservatoria() {
        return conservatoria;
    }

    public void setConservatoria(String conservatoria) {
        this.conservatoria = conservatoria;
    }

    public String getCapitalSocial() {
        return capitalSocial;
    }

    public void setCapitalSocial(String capitalSocial) {
        this.capitalSocial = capitalSocial;
    }

    public String getSoftwareValidationNumber() {
        return softwareValidationNumber;
    }

    public void setSoftwareValidationNumber(String softwareValidationNumber) {
        this.softwareValidationNumber = softwareValidationNumber;
    }

    public String getBanco1() {
        return banco1;
    }

    public void setBanco1(String banco1) {
        this.banco1 = banco1;
    }

    public String getIban1() {
        return iban1;
    }

    public void setIban1(String iban1) {
        this.iban1 = iban1;
    }

    public String getBanco2() {
        return banco2;
    }

    public void setBanco2(String banco2) {
        this.banco2 = banco2;
    }

    public String getIban2() {
        return iban2;
    }

    public void setIban2(String iban2) {
        this.iban2 = iban2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Empresa empresa = (Empresa) o;
        return Objects.equals(id, empresa.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Empresa{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", nif='" + nif + '\'' +
                '}';
    }
}
