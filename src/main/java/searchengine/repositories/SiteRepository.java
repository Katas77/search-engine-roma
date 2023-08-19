package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Status;
import searchengine.model.SiteEntity;


@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Long> {

	boolean existsByUrl(String url);

	SiteEntity findByUrl(String url);


	boolean existsByStatus(Status status);

}



