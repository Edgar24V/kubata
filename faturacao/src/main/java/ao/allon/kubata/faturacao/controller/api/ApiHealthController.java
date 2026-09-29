package ao.allon.kubata.faturacao.controller.api;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ApiHealthController {

    private final ObjectProvider<BuildProperties> buildProperties;

    public ApiHealthController(ObjectProvider<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "kubata-faturacao");
        body.put("timestamp", Instant.now());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        BuildProperties properties = buildProperties.getIfAvailable();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Kubata API");
        body.put("version", properties != null ? properties.getVersion() : "1.0.0-SNAPSHOT");
        body.put("apiVersion", "v1");
        body.put("java", System.getProperty("java.version"));
        body.put("timestamp", Instant.now());
        return ResponseEntity.ok(body);
    }
}
