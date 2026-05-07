package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {

    @NotBlank(message = "O nome é obrigatório")
    @Column(nullable = false)
    private String nome;

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "Email inválido")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(length = 20)
    private String nif;

    @Column(length = 20)
    private String telefone;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_perfis",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "perfil_id")
    )
    private java.util.Set<PerfilAcesso> perfis = new java.util.HashSet<>();

    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Column(name = "mfa_enabled")
    private boolean mfaEnabled = false;

    @Column(name = "ultimo_acesso")
    private java.time.LocalDateTime ultimoAcesso;

    @Column(name = "failed_attempts")
    private int failedAttempts = 0;

    @Column(name = "lockout_end")
    private LocalDateTime lockoutEnd;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "password_provisoria")
    private boolean passwordProvisoria = true;

    @Column(name = "data_expiracao_password")
    private java.time.LocalDate dataExpiracaoPassword;

    private String departamento;
    private String cargo;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "avatar")
    private byte[] avatar;

    @Column(name = "ultimo_ip_login", length = 45)
    private String ultimoIpLogin;

    private boolean superadmin = false;

    private String idioma = "pt-AO";
    private String tema = "VERDE_ADMIN";

    @Column(name = "linhas_por_pagina")
    private int linhasPorPagina = 50;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Override
    public String toString() {
        return nome != null ? nome : (email != null ? email : "Usuário sem identificação");
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public boolean isPasswordProvisoria() {
        return passwordProvisoria;
    }

    public void setPasswordProvisoria(boolean passwordProvisoria) {
        this.passwordProvisoria = passwordProvisoria;
    }

    public java.time.LocalDate getDataExpiracaoPassword() {
        return dataExpiracaoPassword;
    }

    public void setDataExpiracaoPassword(java.time.LocalDate dataExpiracaoPassword) {
        this.dataExpiracaoPassword = dataExpiracaoPassword;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public byte[] getAvatar() {
        return avatar;
    }

    public void setAvatar(byte[] avatar) {
        this.avatar = avatar;
    }

    public String getUltimoIpLogin() {
        return ultimoIpLogin;
    }

    public void setUltimoIpLogin(String ultimoIpLogin) {
        this.ultimoIpLogin = ultimoIpLogin;
    }

    public boolean isSuperadmin() {
        return superadmin;
    }

    public void setSuperadmin(boolean superadmin) {
        this.superadmin = superadmin;
    }

    public String getIdioma() {
        return idioma;
    }

    public void setIdioma(String idioma) {
        this.idioma = idioma;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public int getLinhasPorPagina() {
        return linhasPorPagina;
    }

    public void setLinhasPorPagina(int linhasPorPagina) {
        this.linhasPorPagina = linhasPorPagina;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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

    // Métodos de acesso a perfil removidos

    public String getMfaSecret() {
        return mfaSecret;
    }

    public void setMfaSecret(String mfaSecret) {
        this.mfaSecret = mfaSecret;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public java.time.LocalDateTime getUltimoAcesso() {
        return ultimoAcesso;
    }

    public void setUltimoAcesso(java.time.LocalDateTime ultimoAcesso) {
        this.ultimoAcesso = ultimoAcesso;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public LocalDateTime getLockoutEnd() {
        return lockoutEnd;
    }

    public void setLockoutEnd(LocalDateTime lockoutEnd) {
        this.lockoutEnd = lockoutEnd;
    }

    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        java.util.List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        
        // Permissões granular por perfil removidas
        
        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return getActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return getActive();
    }
}
