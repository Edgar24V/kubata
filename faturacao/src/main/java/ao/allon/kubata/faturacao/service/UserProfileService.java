package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.UserProfileEntity;
import ao.allon.kubata.faturacao.profile.model.UserType;
import ao.allon.kubata.faturacao.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserProfileService {
    private final UserProfileRepository repo;

    public UserProfileService(UserProfileRepository repo) {
        this.repo = repo;
    }

    public List<UserProfileEntity> findAll() { return repo.findAll(); }
    public UserProfileEntity save(UserProfileEntity e) { return repo.save(e); }
    public void delete(Long id) { repo.deleteById(id); }
    public long countByType(UserType t) { return repo.countByTipo(t); }

    public static String toCsv(List<String> list) {
        return list == null ? "" : String.join(",", list);
    }
    public static List<String> fromCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",")).map(String::trim).collect(Collectors.toList());
    }
}
