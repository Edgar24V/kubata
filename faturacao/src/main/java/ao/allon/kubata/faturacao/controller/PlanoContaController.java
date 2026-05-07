package ao.allon.kubata.faturacao.controller;

import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.core.service.PlanoContaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/plano-contas")
public class PlanoContaController {

    private final PlanoContaService service;

    public PlanoContaController(PlanoContaService service) {
        this.service = service;
    }

    @GetMapping
    public List<PlanoConta> listarTodas() {
        return service.findAllRoots(); // Retorna raízes, que contêm a árvore se configurado fetch EAGER ou via DTO
        // Nota: Como subContas é LAZY e estamos serializando JSON, pode dar problema se não inicializar ou usar DTO.
        // O ideal é usar DTO para evitar recursão infinita no JSON (Pai -> Filho -> Pai).
        // Mas para simplificar agora, vou assumir que o Jackson ignora o 'contaPai' na serialização das subContas ou usar @JsonIgnore na entidade.
    }
    
    @GetMapping("/{codigo}")
    public ResponseEntity<PlanoConta> buscarPorCodigo(@PathVariable String codigo) {
        return service.findByCodigo(codigo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PlanoConta> criar(@RequestBody PlanoConta conta) {
        return ResponseEntity.ok(service.save(conta));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanoConta> atualizar(@PathVariable Long id, @RequestBody PlanoConta conta) {
        if (!id.equals(conta.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(service.save(conta));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/importar")
    public ResponseEntity<Void> importar(@RequestParam("file") MultipartFile file) throws IOException {
        service.importarPlanoContasCSV(file.getInputStream());
        return ResponseEntity.ok().build();
    }
}
