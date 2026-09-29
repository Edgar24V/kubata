package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.SerieDocumento;
import ao.allon.kubata.core.domain.SerieDocumento.TipoDocumentoSAFT;
import ao.allon.kubata.core.repository.SerieDocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SerieDocumentoService {

    private final SerieDocumentoRepository repository;

    @Transactional
    public List<SerieDocumento> criarNovasSeries(Empresa empresa, List<TipoDocumentoSAFT> tipos, String nomeSerie, Integer ano, Long serieExemploId) {
        List<SerieDocumento> novasSeries = new ArrayList<>();
        
        Optional<SerieDocumento> exemploOpt = Optional.empty();
        if (serieExemploId != null) {
            exemploOpt = repository.findById(serieExemploId);
        }

        for (TipoDocumentoSAFT tipo : tipos) {
            // Busca se já existe uma série com esse nome e ano para o tipo
            Optional<SerieDocumento> existente = repository.findByEmpresaIdAndTipoDocumentoAndSerieAndExercicio(
                    empresa.getId(), tipo, nomeSerie, ano);

            if (existente.isPresent()) continue;

            SerieDocumento.SerieDocumentoBuilder builder = SerieDocumento.builder()
                    .empresa(empresa)
                    .tipoDocumento(tipo)
                    .serie(nomeSerie)
                    .descricao("Série " + nomeSerie + " - " + ano)
                    .exercicio(ano)
                    .dataInicio(LocalDate.of(ano, 1, 1))
                    .dataFim(LocalDate.of(ano, 12, 31))
                    .estado(SerieDocumento.EstadoSerie.ACTIVA)
                    .criadoEm(LocalDateTime.now())
                    .ultimoNumero(0L)
                    .numeroInicial(1L);

            // Se houver série de exemplo, copia atributos (prefixo, formato, etc.)
            if (exemploOpt.isPresent()) {
                SerieDocumento exemplo = exemploOpt.get();
                builder.prefixo(exemplo.getPrefixo())
                       .formatoNumero(exemplo.getFormatoNumero())
                       .predefinida(exemplo.getPredefinida());
            } else {
                builder.prefixo(tipo.getPrefixo())
                       .formatoNumero("{PREFIXO} {SERIE}/{NUMERO}");
            }

            novasSeries.add(repository.save(builder.build()));
        }
        
        return novasSeries;
    }


    /**
     * Replica séries de um exercício para outro, preservando a configuração
     * documental e reiniciando a sequência/estado de registo AGT.
     *
     * <p>Séries que já existam no destino são ignoradas, mantendo a operação
     * idempotente para o assistente de instalação.</p>
     */
    @Transactional
    public List<SerieDocumento> replicarParaExercicio(
            Empresa empresa,
            List<SerieDocumento> origem,
            Integer novoAno
    ) {
        List<SerieDocumento> criadas = new ArrayList<>();
        if (empresa == null || empresa.getId() == null || origem == null || novoAno == null) {
            return criadas;
        }

        for (SerieDocumento base : origem) {
            if (base == null || base.getTipoDocumento() == null || base.getSerie() == null) {
                continue;
            }

            Optional<SerieDocumento> existente =
                    repository.findByEmpresaIdAndTipoDocumentoAndSerieAndExercicio(
                            empresa.getId(),
                            base.getTipoDocumento(),
                            base.getSerie(),
                            novoAno
                    );

            if (existente.isPresent()) {
                continue;
            }

            SerieDocumento nova = SerieDocumento.builder()
                    .empresa(empresa)
                    .tipoDocumento(base.getTipoDocumento())
                    .serie(base.getSerie())
                    .descricao(base.getDescricao() == null
                            ? "Série " + base.getSerie() + " - " + novoAno
                            : base.getDescricao().replace(
                                    base.getExercicio() == null ? "" : String.valueOf(base.getExercicio()),
                                    String.valueOf(novoAno)
                            ))
                    .exercicio(novoAno)
                    .numeroInicial(base.getNumeroInicial() == null ? 1L : base.getNumeroInicial())
                    .ultimoNumero(0L)
                    .prefixo(base.getPrefixo())
                    .formatoNumero(base.getFormatoNumero())
                    .dataInicio(LocalDate.of(novoAno, 1, 1))
                    .dataFim(LocalDate.of(novoAno, 12, 31))
                    .estado(SerieDocumento.EstadoSerie.ACTIVA)
                    .predefinida(Boolean.TRUE.equals(base.getPredefinida()))
                    .registadaAGT(false)
                    .criadoEm(LocalDateTime.now())
                    .build();

            criadas.add(repository.save(nova));
        }

        return criadas;
    }

    public List<SerieDocumento> listarPorEmpresa(Long empresaId) {
        return repository.findByEmpresaId(empresaId);
    }
}
