package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Website;
import searchengine.model.Status;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Website, Long> {
   Optional<Website> findByUrl(String url);
    boolean existsByStatus(Status status);


}



