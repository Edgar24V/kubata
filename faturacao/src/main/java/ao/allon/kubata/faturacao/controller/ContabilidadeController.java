package ao.allon.kubata.faturacao.controller;

import ao.allon.kubata.core.domain.LancamentoContabil;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.core.service.ContabilidadeService.BalanceteItemDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/contabilidade")
public class ContabilidadeController {

    private final ContabilidadeService service;

    public ContabilidadeController(ContabilidadeService service) {
        this.service = service;
    }

    @GetMapping("/balancete")
    public ResponseEntity<List<BalanceteItemDTO>> getBalancete(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(service.gerarBalancete(data));
    }

    // Endpoint para lançamentos manuais (opcional, já que o sistema deve ser integrado)
    // Mas útil para testes ou ajustes.
}
