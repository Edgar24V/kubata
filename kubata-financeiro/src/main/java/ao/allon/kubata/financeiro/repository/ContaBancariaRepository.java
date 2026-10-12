package ao.allon.kubata.financeiro.repository;

import ao.allon.kubata.financeiro.domain.ContaBancaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContaBancariaRepository extends JpaRepository<ContaBancaria, Long> {

    Optional<ContaBancaria> findByIsPrincipalTrue();

    List<ContaBancaria> findByIsAtivoTrue();

    boolean existsByIban(String iban);

    boolean existsByIbanIgnoreCase(String iban);

    boolean existsByIbanIgnoreCaseAndIdNot(String iban, Long id);

    boolean existsByNumeroContaIgnoreCase(String numeroConta);

    boolean existsByNumeroContaIgnoreCaseAndIdNot(String numeroConta, Long id);
}
