package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.domain.Moeda;
import ao.allon.kubata.core.repository.MoedaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MoedaService {

    public static final String AOA = "AOA";

    private final MoedaRepository moedaRepository;

    public MoedaService(MoedaRepository moedaRepository) {
        this.moedaRepository = moedaRepository;
    }

    public List<Moeda> findAtivas() {
        return moedaRepository.findAll().stream()
                .filter(m -> Boolean.TRUE.equals(m.getActiva()))
                .sorted(Comparator.comparing(Moeda::getCodigoISO))
                .collect(Collectors.toList());
    }

    public Optional<Moeda> findByCodigo(String codigoIso) {
        return moedaRepository.findByCodigoISO(codigoIso);
    }

    public Moeda base() {
        Optional<Moeda> porFlag = moedaRepository.findAll().stream()
                .filter(m -> Boolean.TRUE.equals(m.getMoedaBase()) && Boolean.TRUE.equals(m.getActiva()))
                .findFirst();
        if (porFlag.isPresent()) return porFlag.get();
        Optional<Moeda> aoa = moedaRepository.findByCodigoISO(AOA);
        if (aoa.isPresent()) return aoa.get();
        return criarAoaPadrao();
    }

    @Transactional
    public Moeda save(Moeda m) {
        return moedaRepository.save(m);
    }

    public BigDecimal taxaParaKwanza(Moeda m) {
        if (m == null || m.getTaxaCambio() == null) return BigDecimal.ONE;
        return m.getTaxaCambio();
    }

    private Moeda criarAoaPadrao() {
        Moeda m = new Moeda();
        m.setCodigoISO(AOA);
        m.setNome("Kwanza");
        m.setSimbolo("Kz");
        m.setTaxaCambio(BigDecimal.ONE);
        m.setMoedaBase(true);
        m.setCasasDecimais(2);
        m.setActiva(true);
        return moedaRepository.save(m);
    }
}
