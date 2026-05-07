package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.ItemFatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemFaturaRepository extends JpaRepository<ItemFatura, Long> {
}
