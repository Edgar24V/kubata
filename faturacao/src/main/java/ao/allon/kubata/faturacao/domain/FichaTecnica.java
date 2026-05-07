package ao.allon.kubata.faturacao.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fichas_tecnicas")
public class FichaTecnica extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "produto_id", nullable = false, unique = true)
    private Produto produto;

    @OneToMany(mappedBy = "fichaTecnica", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FichaTecnicaItem> itens = new ArrayList<>();

    private String observacoes;

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public List<FichaTecnicaItem> getItens() {
        return itens;
    }

    public void setItens(List<FichaTecnicaItem> itens) {
        this.itens = itens;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public void addItem(FichaTecnicaItem item) {
        itens.add(item);
        item.setFichaTecnica(this);
    }
}
