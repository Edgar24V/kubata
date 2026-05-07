package ao.allon.kubata.faturacao.profile.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserProfile {
    private Long id;
    private String nome;
    private String email;
    private UserType tipo;
    private boolean ativo;
    private LocalDateTime criadoEm;
    private String telefone;
    private String departamento;
    private List<String> permissoes = new ArrayList<>();
    private List<String> atividadesRecentes = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public UserType getTipo() { return tipo; }
    public void setTipo(UserType tipo) { this.tipo = tipo; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
    public List<String> getPermissoes() { return permissoes; }
    public void setPermissoes(List<String> permissoes) { this.permissoes = permissoes; }
    public List<String> getAtividadesRecentes() { return atividadesRecentes; }
    public void setAtividadesRecentes(List<String> atividadesRecentes) { this.atividadesRecentes = atividadesRecentes; }
}
