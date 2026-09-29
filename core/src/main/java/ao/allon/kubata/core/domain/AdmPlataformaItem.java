package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "adm_plataforma_item", uniqueConstraints = @UniqueConstraint(
        name = "uk_adm_plataforma_tipo_codigo", columnNames = {"tipo", "codigo"}))
public class AdmPlataformaItem extends BaseEntity {
    @Column(name="tipo",nullable=false,length=40) private String tipo;
    @Column(name="codigo",nullable=false,length=120) private String codigo;
    @Column(name="nome",nullable=false,length=255) private String nome;
    @Column(name="estado",nullable=false,length=40) private String estado="ACTIVO";
    @Column(name="descricao",length=1000) private String descricao;
    @Column(name="config_json",columnDefinition="TEXT") private String configJson;
    @Column(name="schedule_seconds") private Integer scheduleSeconds;
    @Column(name="next_run_at") private LocalDateTime nextRunAt;
    @Column(name="last_run_at") private LocalDateTime lastRunAt;
    @Column(name="attempts") private Integer attempts=0;
    @Column(name="last_message",length=2000) private String lastMessage;
    @Column(name="owner_username",length=120) private String ownerUsername;
    @Column(name="resource_path",length=1000) private String resourcePath;
    public String getTipo(){return tipo;} public void setTipo(String v){tipo=v;}
    public String getCodigo(){return codigo;} public void setCodigo(String v){codigo=v;}
    public String getNome(){return nome;} public void setNome(String v){nome=v;}
    public String getEstado(){return estado;} public void setEstado(String v){estado=v;}
    public String getDescricao(){return descricao;} public void setDescricao(String v){descricao=v;}
    public String getConfigJson(){return configJson;} public void setConfigJson(String v){configJson=v;}
    public Integer getScheduleSeconds(){return scheduleSeconds;} public void setScheduleSeconds(Integer v){scheduleSeconds=v;}
    public LocalDateTime getNextRunAt(){return nextRunAt;} public void setNextRunAt(LocalDateTime v){nextRunAt=v;}
    public LocalDateTime getLastRunAt(){return lastRunAt;} public void setLastRunAt(LocalDateTime v){lastRunAt=v;}
    public Integer getAttempts(){return attempts;} public void setAttempts(Integer v){attempts=v;}
    public String getLastMessage(){return lastMessage;} public void setLastMessage(String v){lastMessage=v;}
    public String getOwnerUsername(){return ownerUsername;} public void setOwnerUsername(String v){ownerUsername=v;}
    public String getResourcePath(){return resourcePath;} public void setResourcePath(String v){resourcePath=v;}
}