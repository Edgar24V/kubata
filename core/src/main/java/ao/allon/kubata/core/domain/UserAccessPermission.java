package ao.allon.kubata.core.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "user_access_permission", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_mod_op", columnNames = {"user_id", "modulo", "opcao"})
})
public class UserAccessPermission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "modulo", length = 64, nullable = false)
    private String modulo;

    @Column(name = "opcao", length = 64, nullable = false)
    private String opcao;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getModulo() {
        return modulo;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }

    public String getOpcao() {
        return opcao;
    }

    public void setOpcao(String opcao) {
        this.opcao = opcao;
    }
}
