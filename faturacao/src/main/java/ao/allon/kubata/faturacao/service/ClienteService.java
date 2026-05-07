package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.repository.ClienteRepository;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final FaturaRepository faturaRepository;

    public ClienteService(ClienteRepository clienteRepository, FaturaRepository faturaRepository) {
        this.clienteRepository = clienteRepository;
        this.faturaRepository = faturaRepository;
    }

    public List<Cliente> findAll() {
        return clienteRepository.findAll();
    }

    public Optional<Cliente> findById(Long id) {
        if (id == null) return Optional.empty();
        return clienteRepository.findById(id);
    }

    @Transactional
    public Cliente save(Cliente cliente) {
        String nifMsg = ao.allon.kubata.faturacao.util.AngolaValidationUtils.validateNifMessage(cliente.getNif());
        if (nifMsg != null) {
            throw new IllegalArgumentException(nifMsg);
        }
        
        if (cliente.getId() != null) {
            // Verificar se o cliente já possui faturas emitidas antes de permitir alteração de NIF
            Cliente existente = clienteRepository.findById(cliente.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
            
            if (existente.getNif() != null && !existente.getNif().equals(cliente.getNif())) {
                boolean possuiFaturas = faturaRepository.existsByClienteIdAndStatusIn(
                        cliente.getId(), 
                        List.of(ao.allon.kubata.faturacao.domain.enums.StatusFatura.EMITIDA, 
                                ao.allon.kubata.faturacao.domain.enums.StatusFatura.PAGA,
                                ao.allon.kubata.faturacao.domain.enums.StatusFatura.CANCELADA)
                );
                
                if (possuiFaturas) {
                    throw new IllegalStateException("Não é possível alterar o NIF de um cliente que já possui documentos fiscais emitidos (Norma AGT).");
                }
            }
        }

        if (cliente.getId() == null) {
            if (clienteRepository.existsByNif(cliente.getNif())) {
                throw new IllegalArgumentException("Já existe um cliente com este NIF: " + cliente.getNif());
            }
        } else {
            clienteRepository.findByNif(cliente.getNif())
                    .filter(outro -> !outro.getId().equals(cliente.getId()))
                    .ifPresent(outro -> {
                        throw new IllegalArgumentException("Já existe um cliente com este NIF: " + cliente.getNif());
                    });
        }
        return clienteRepository.save(cliente);
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) throw new IllegalArgumentException("ID cannot be null");
        clienteRepository.deleteById(id);
    }

    public int countTotalClientes() {
        return (int) clienteRepository.count();
    }
}
