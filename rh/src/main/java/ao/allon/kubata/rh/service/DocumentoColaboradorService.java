package ao.allon.kubata.rh.service;

import ao.allon.kubata.rh.domain.DocumentoColaborador;
import ao.allon.kubata.rh.domain.Colaborador;
import ao.allon.kubata.rh.repository.DocumentoColaboradorRepository;
import ao.allon.kubata.rh.repository.ColaboradorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DocumentoColaboradorService {

    @Autowired
    private DocumentoColaboradorRepository repository;

    @Autowired
    private ColaboradorRepository colaboradorRepository;

    public List<DocumentoColaborador> findAll() {
        return repository.findAll();
    }

    public Optional<DocumentoColaborador> findById(Long id) {
        return repository.findById(id);
    }

    public DocumentoColaborador save(DocumentoColaborador documento) {
        if (documento.getColaborador() == null || documento.getColaborador().getId() == null) {
            throw new IllegalArgumentException("Colaborador é obrigatório");
        }

        if (documento.getTipo() == null) {
            throw new IllegalArgumentException("Tipo de documento é obrigatório");
        }

        if (documento.getNomeDocumento() == null || documento.getNomeDocumento().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do documento é obrigatório");
        }

        if (documento.getCaminhoArquivo() == null || documento.getCaminhoArquivo().trim().isEmpty()) {
            throw new IllegalArgumentException("Caminho do arquivo é obrigatório");
        }

        // Verifica se colaborador existe
        Colaborador colaborador = colaboradorRepository.findById(documento.getColaborador().getId())
            .orElseThrow(() -> new IllegalArgumentException("Colaborador não encontrado"));

        documento.setColaborador(colaborador);

        // Define data de upload se não estiver definida
        if (documento.getDataUpload() == null) {
            documento.setDataUpload(LocalDate.now());
        }

        // Define ativo como true por padrão se não estiver definido
        if (documento.getActive() == null) {
            documento.setActive(true);
        }

        return repository.save(documento);
    }

    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Documento não encontrado");
        }
        repository.deleteById(id);
    }

    public void ativar(Long id) {
        DocumentoColaborador documento = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado"));
        documento.setActive(true);
        repository.save(documento);
    }

    public void desativar(Long id) {
        DocumentoColaborador documento = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado"));
        documento.setActive(false);
        repository.save(documento);
    }

    public List<DocumentoColaborador> findByColaborador(Long colaboradorId) {
        return repository.findByColaboradorIdOrderByDataUploadDesc(colaboradorId);
    }

    public List<DocumentoColaborador> findByTipo(DocumentoColaborador.TipoDocumento tipo) {
        return repository.findByTipoOrderByDataUploadDesc(tipo);
    }

    public List<DocumentoColaborador> findAtivos() {
        return repository.findByActiveTrueOrderByDataUploadDesc();
    }

    public List<DocumentoColaborador> findInativos() {
        return repository.findByActiveFalseOrderByDataUploadDesc();
    }

    public List<DocumentoColaborador> findDocumentosAExpirar(LocalDate dataInicio, LocalDate dataFim) {
        return repository.findDocumentosAExpirar(dataInicio, dataFim);
    }

    public List<DocumentoColaborador> findDocumentosExpirados() {
        return repository.findDocumentosExpirados(LocalDate.now());
    }

    public List<DocumentoColaborador> searchByTermo(String termo) {
        return repository.searchByTermo(termo);
    }

    public List<DocumentoColaborador> findByFiltros(Long colaboradorId, DocumentoColaborador.TipoDocumento tipo, 
                                                     Boolean ativo, String nomeDocumento) {
        return repository.findByFiltros(colaboradorId, tipo, ativo, nomeDocumento);
    }

    public long countByColaborador(Long colaboradorId) {
        return repository.countByColaboradorAndAtivo(colaboradorId);
    }
}
