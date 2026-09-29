package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.AdmPlataformaItem;
import ao.allon.kubata.core.repository.AdmPlataformaItemRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PersonalizacaoRuntimeService {
    private final AdmPlataformaItemRepository repository;
    public PersonalizacaoRuntimeService(AdmPlataformaItemRepository repository){this.repository=repository;}
    public List<AdmPlataformaItem> active(String type){
        return repository.findByTipoOrderByUpdatedAtDesc("PERSONALIZACAO").stream()
                .filter(i->Boolean.TRUE.equals(i.getActive())&&"ACTIVO".equalsIgnoreCase(i.getEstado()))
                .filter(i->i.getCodigo()!=null&&i.getCodigo().toUpperCase().startsWith(type.toUpperCase()+"_"))
                .toList();
    }
    public boolean enabled(String type,String code){
        return active(type).stream().anyMatch(i->i.getCodigo().equalsIgnoreCase(type+"_"+code));
    }
}