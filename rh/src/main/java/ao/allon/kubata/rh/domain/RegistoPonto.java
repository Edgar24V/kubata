package ao.allon.kubata.rh.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rh_registo_ponto",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_registo_ponto_colaborador_data", 
                           columnNames = {"colaborador_id", "data"})
       })
public class RegistoPonto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "colaborador_id", nullable = false)
    private Colaborador colaborador;

    @NotNull(message = "Data é obrigatória")
    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "hora_entrada")
    private LocalDateTime horaEntrada;

    @Column(name = "hora_saida")
    private LocalDateTime horaSaida;

    @Column(name = "hora_entrada2")
    private LocalDateTime horaEntrada2; // Para intervalos de almoço

    @Column(name = "hora_saida2")
    private LocalDateTime horaSaida2;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", length = 20)
    private OrigemPonto origem = OrigemPonto.MANUAL;

    @Column(name = "dispositivo", length = 100)
    private String dispositivo; // Relógio de ponto, app, etc

    @Size(max = 500, message = "Observações não podem exceder 500 caracteres")
    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprovado_por")
    private ao.allon.kubata.core.domain.User aprovadoPor;

    @Column(name = "data_aprovacao")
    private LocalDateTime dataAprovacao;

    public enum OrigemPonto {
        MANUAL,
        RELOGIO_PONTO,
        APP_MOBILE,
        WEB
    }

    public RegistoPonto() {}

    public Colaborador getColaborador() {
        return colaborador;
    }

    public void setColaborador(Colaborador colaborador) {
        this.colaborador = colaborador;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalDateTime getHoraEntrada() {
        return horaEntrada;
    }

    public void setHoraEntrada(LocalDateTime horaEntrada) {
        this.horaEntrada = horaEntrada;
    }

    public LocalDateTime getHoraSaida() {
        return horaSaida;
    }

    public void setHoraSaida(LocalDateTime horaSaida) {
        this.horaSaida = horaSaida;
    }

    public LocalDateTime getHoraEntrada2() {
        return horaEntrada2;
    }

    public void setHoraEntrada2(LocalDateTime horaEntrada2) {
        this.horaEntrada2 = horaEntrada2;
    }

    public LocalDateTime getHoraSaida2() {
        return horaSaida2;
    }

    public void setHoraSaida2(LocalDateTime horaSaida2) {
        this.horaSaida2 = horaSaida2;
    }

    public OrigemPonto getOrigem() {
        return origem;
    }

    public void setOrigem(OrigemPonto origem) {
        this.origem = origem;
    }

    public String getDispositivo() {
        return dispositivo;
    }

    public void setDispositivo(String dispositivo) {
        this.dispositivo = dispositivo;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public ao.allon.kubata.core.domain.User getAprovadoPor() {
        return aprovadoPor;
    }

    public void setAprovadoPor(ao.allon.kubata.core.domain.User aprovadoPor) {
        this.aprovadoPor = aprovadoPor;
    }

    public LocalDateTime getDataAprovacao() {
        return dataAprovacao;
    }

    public void setDataAprovacao(LocalDateTime dataAprovacao) {
        this.dataAprovacao = dataAprovacao;
    }

    @Override
    public String toString() {
        return colaborador != null ? colaborador.getNomeCompleto() + " - " + data : "RegistoPonto";
    }
}
