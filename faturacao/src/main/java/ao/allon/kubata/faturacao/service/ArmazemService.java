package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Armazem;
import ao.allon.kubata.faturacao.repository.ArmazemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ArmazemService {

    private final ArmazemRepository armazemRepository;

    public ArmazemService(ArmazemRepository armazemRepository) {
        this.armazemRepository = armazemRepository;
    }

    public List<Armazem> findAll() {
        return armazemRepository.findAll();
    }

    public Optional<Armazem> findById(Long id) {
        return armazemRepository.findById(id);
    }

    @Transactional
    public Armazem save(Armazem armazem) {
        if (armazem.getIsPrincipal()) {
            // Se este está sendo definido como principal, remove o status dos outros
            armazemRepository.findByIsPrincipalTrue().ifPresent(current -> {
                if (!current.getId().equals(armazem.getId())) {
                    current.setIsPrincipal(false);
                    armazemRepository.save(current);
                }
            });
        } else {
            // Garante que existe pelo menos um principal se for o primeiro
            if (armazemRepository.count() == 0) {
                armazem.setIsPrincipal(true);
            }
        }
        return armazemRepository.save(armazem);
    }

    @Transactional
    public void delete(Long id) {
        armazemRepository.findById(id).ifPresent(armazem -> {
            if (armazem.getIsPrincipal()) {
                throw new IllegalStateException("Não é possível excluir o armazém principal. Defina outro como principal antes.");
            }
            armazemRepository.delete(armazem);
        });
    }

    @Transactional
    public void setPrincipal(Armazem armazem) {
        armazemRepository.findByIsPrincipalTrue().ifPresent(current -> {
            current.setIsPrincipal(false);
            armazemRepository.save(current);
        });
        armazem.setIsPrincipal(true);
        armazemRepository.save(armazem);
    }
}
