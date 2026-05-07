package ao.allon.kubata.vendas.service;

import ao.allon.kubata.vendas.domain.Cliente;
import ao.allon.kubata.vendas.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public Cliente save(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public Optional<Cliente> findById(Long id) {
        return clienteRepository.findById(id);
    }

    public Optional<Cliente> findByNif(String nif) {
        return clienteRepository.findByNif(nif);
    }

    public List<Cliente> findAll() {
        return clienteRepository.findAll();
    }

    public List<Cliente> findByNome(String nome) {
        return clienteRepository.findByNomeContainingIgnoreCase(nome);
    }

    public void delete(Long id) {
        clienteRepository.deleteById(id);
    }

    public boolean existsByNif(String nif) {
        return clienteRepository.existsByNif(nif);
    }
}
