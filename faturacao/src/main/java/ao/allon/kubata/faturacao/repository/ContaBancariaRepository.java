package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.ContaBancaria;
import ao.allon.kubata.faturacao.domain.enums.BancoAngola;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContaBancariaRepository extends JpaRepository<ContaBancaria, Long> {
    Optional<ContaBancaria> findByIban(String iban);
    List<ContaBancaria> findByBanco(BancoAngola banco);
    boolean existsByIban(String iban);
}
