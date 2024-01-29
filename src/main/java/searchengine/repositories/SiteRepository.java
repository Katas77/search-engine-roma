package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Website;
import searchengine.model.Status;


@Repository
public interface SiteRepository extends JpaRepository<Website, Long> {

    Website findByUrl(String url);

    boolean existsByStatus(Status status);


}



