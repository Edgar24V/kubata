package ao.allon.kubata.rh.service;

import ao.allon.kubata.rh.domain.PedidoFerias;
import ao.allon.kubata.rh.domain.Colaborador;
import ao.allon.kubata.rh.repository.PedidoFeriasRepository;
import ao.allon.kubata.rh.repository.ColaboradorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PedidoFeriasService {

    @Autowired
    private PedidoFeriasRepository repository;

    @Autowired
    private ColaboradorRepository colaboradorRepository;

    public List<PedidoFerias> findAll() {
        return repository.findAll();
    }

    public Optional<PedidoFerias> findById(Long id) {
        return repository.findById(id);
    }

    public PedidoFerias save(PedidoFerias pedido) {
        if (pedido.getColaborador() == null || pedido.getColaborador().getId() == null) {
            throw new IllegalArgumentException("Colaborador é obrigatório");
        }

        if (pedido.getDataInicio() == null || pedido.getDataFim() == null) {
            throw new IllegalArgumentException("Datas de início e fim são obrigatórias");
        }

        if (pedido.getDataInicio().isAfter(pedido.getDataFim())) {
            throw new IllegalArgumentException("Data de início não pode ser posterior à data de fim");
        }

        if (pedido.getQuantidadeDias() == null || pedido.getQuantidadeDias() <= 0) {
            throw new IllegalArgumentException("Quantidade de dias deve ser maior que zero");
        }

        // Verifica se colaborador existe
        Colaborador colaborador = colaboradorRepository.findById(pedido.getColaborador().getId())
            .orElseThrow(() -> new IllegalArgumentException("Colaborador não encontrado"));

        pedido.setColaborador(colaborador);

        return repository.save(pedido);
    }

    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Pedido de férias não encontrado");
        }
        
        PedidoFerias pedido = repository.findById(id).orElse(null);
        if (pedido != null && pedido.getEstado() == PedidoFerias.EstadoPedido.APROVADO) {
            throw new IllegalArgumentException("Não é possível excluir um pedido já aprovado");
        }
        
        repository.deleteById(id);
    }

    public List<PedidoFerias> findByColaborador(Long colaboradorId) {
        return repository.findByColaboradorIdOrderByDataInicioDesc(colaboradorId);
    }

    public List<PedidoFerias> findByDataInicioBetween(LocalDate dataInicio, LocalDate dataFim) {
        return repository.findByDataInicioBetweenOrderByDataInicio(dataInicio, dataFim);
    }

    public List<PedidoFerias> findByEstado(PedidoFerias.EstadoPedido estado) {
        return repository.findByEstadoOrderByDataInicioDesc(estado);
    }

    public List<PedidoFerias> findPendentes() {
        return repository.findByEstadoOrderByDataInicioDesc(PedidoFerias.EstadoPedido.PENDENTE);
    }

    public void aprovar(Long id, Long aprovadoPor) {
        PedidoFerias pedido = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Pedido de férias não encontrado"));

        if (pedido.getEstado() != PedidoFerias.EstadoPedido.PENDENTE) {
            throw new IllegalArgumentException("Apenas pedidos pendentes podem ser aprovados");
        }

        pedido.setEstado(PedidoFerias.EstadoPedido.APROVADO);
        pedido.setDataAprovacao(LocalDate.now());
        // TODO: Set aprovadoPor when user authentication is ready
        repository.save(pedido);
    }

    public void rejeitar(Long id, String motivoRejeicao, Long rejeitadoPor) {
        PedidoFerias pedido = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Pedido de férias não encontrado"));

        if (pedido.getEstado() != PedidoFerias.EstadoPedido.PENDENTE) {
            throw new IllegalArgumentException("Apenas pedidos pendentes podem ser rejeitados");
        }

        pedido.setEstado(PedidoFerias.EstadoPedido.REJEITADO);
        pedido.setMotivoRejeicao(motivoRejeicao);
        repository.save(pedido);
    }

    public void cancelar(Long id) {
        PedidoFerias pedido = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Pedido de férias não encontrado"));

        if (pedido.getEstado() == PedidoFerias.EstadoPedido.APROVADO && 
            pedido.getDataInicio().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Não é possível cancelar férias já iniciadas");
        }

        pedido.setEstado(PedidoFerias.EstadoPedido.CANCELADO);
        repository.save(pedido);
    }

    public int calcularDiasDisponiveis(Long colaboradorId) {
        // TODO: Implementar cálculo baseado no período aquisitivo
        // Por enquanto retorna 22 dias (padrão em Angola)
        return 22;
    }

    public List<PedidoFerias> findByColaboradorAndAno(Long colaboradorId, int ano) {
        LocalDate inicioAno = LocalDate.of(ano, 1, 1);
        LocalDate fimAno = LocalDate.of(ano, 12, 31);
        return repository.findByFiltros(colaboradorId, inicioAno, fimAno, null);
    }
}
