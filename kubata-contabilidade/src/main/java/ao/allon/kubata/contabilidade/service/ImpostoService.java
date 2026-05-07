package ao.allon.kubata.contabilidade.service;

import ao.allon.kubata.contabilidade.domain.Imposto;
import ao.allon.kubata.contabilidade.repository.ImpostoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ImpostoService {

    private final ImpostoRepository impostoRepository;

    public ImpostoService(ImpostoRepository impostoRepository) {
        this.impostoRepository = impostoRepository;
    }

    public Imposto save(Imposto imposto) {
        return impostoRepository.save(imposto);
    }

    public Optional<Imposto> findById(Long id) {
        return impostoRepository.findById(id);
    }

    public Optional<Imposto> findByCodigo(String codigo) {
        return impostoRepository.findByCodigo(codigo);
    }

    public List<Imposto> findAllAtivos() {
        return impostoRepository.findByIsAtivoTrue();
    }

    public List<Imposto> findAll() {
        return impostoRepository.findAll();
    }

    public void delete(Long id) {
        impostoRepository.deleteById(id);
    }

    public boolean existsByCodigo(String codigo) {
        return impostoRepository.existsByCodigo(codigo);
    }
}
