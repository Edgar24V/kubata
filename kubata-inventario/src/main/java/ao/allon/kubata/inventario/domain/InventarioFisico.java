package ao.allon.kubata.inventario.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import ao.allon.kubata.inventario.enums.EstadoInventario;
import ao.allon.kubata.inventario.enums.TipoInventario;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventarios_fisicos")
public class InventarioFisico extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoInventario tipo = TipoInventario.TOTAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoInventario estado = EstadoInventario.PREPARADO;

    @Column(name = "data_inventario", nullable = false)
    private LocalDate dataInventario = LocalDate.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "armazem_id")
    private Armazem armazem;

    @Column(length = 500)
    private String observacoes;

    @OneToMany(mappedBy = "inventario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventarioFisicoLinha> linhas = new ArrayList<>();

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public TipoInventario getTipo() { return tipo; }
    public void setTipo(TipoInventario tipo) { this.tipo = tipo; }
    public EstadoInventario getEstado() { return estado; }
    public void setEstado(EstadoInventario estado) { this.estado = estado; }
    public LocalDate getDataInventario() { return dataInventario; }
    public void setDataInventario(LocalDate dataInventario) { this.dataInventario = dataInventario; }
    public Armazem getArmazem() { return armazem; }
    public void setArmazem(Armazem armazem) { this.armazem = armazem; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public List<InventarioFisicoLinha> getLinhas() { return linhas; }
    public void setLinhas(List<InventarioFisicoLinha> linhas) { this.linhas = linhas; }
    public void addLinha(InventarioFisicoLinha linha) {
        linhas.add(linha);
        linha.setInventario(this);
    }
}
