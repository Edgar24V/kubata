package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.AdmPlataformaItem;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PlataformaDocumentService {
    private final PlataformaAutomationService automation;
    public PlataformaDocumentService(PlataformaAutomationService automation){this.automation=automation;}
    public AdmPlataformaItem store(Path source,Path repositoryDir,String owner)throws IOException{
        if(source==null||!Files.isRegularFile(source))throw new IOException("Ficheiro de origem inválido.");
        Files.createDirectories(repositoryDir);
        String clean=source.getFileName().toString().replaceAll("[^A-Za-z0-9._-]","_");
        Path target=repositoryDir.resolve(UUID.randomUUID()+"_"+clean);
        Files.copy(source,target,StandardCopyOption.REPLACE_EXISTING);
        return automation.save("DOCUMENTO",UUID.randomUUID().toString(),clean,"ACTIVO",
                "Documento importado em "+LocalDateTime.now(),
                "{\"tamanho\":"+Files.size(target)+"}",null,owner,target.toAbsolutePath().toString());
    }
    public void delete(AdmPlataformaItem item)throws IOException{if(item.getResourcePath()!=null)Files.deleteIfExists(Paths.get(item.getResourcePath()));automation.remove(item);}
}