package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.Devolucao;
import ao.allon.kubata.faturacao.domain.enums.StatusDevolucao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DevolucaoRepository extends JpaRepository<Devolucao, Long> {
    
    List<Devolucao> findByStatus(StatusDevolucao status);
    
    @Query("SELECT d FROM Devolucao d WHERE d.dataSolicitacao BETWEEN :startDate AND :endDate")
    List<Devolucao> findByPeriodo(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT d FROM Devolucao d WHERE d.cliente.id = :clienteId")
    List<Devolucao> findByCliente(@Param("clienteId") Long clienteId);
}
