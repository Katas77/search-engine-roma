package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Website;

import java.util.List;

@Transactional
@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {

	Integer countBySiteEntity(Website siteEntity);

	@Query(value = "SELECT l.* FROM Lemma l WHERE l.lemma IN :lemmas AND l.site_id = :site", nativeQuery = true)
	List<Lemma> findLemmasBySite(@Param("lemmas") List<String> lemmas, @Param("site") Website site);
}