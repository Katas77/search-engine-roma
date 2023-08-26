package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;
import java.util.Set;

@Transactional
@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Long> {

	@Query(value = "SELECT i.* FROM search_index i WHERE i.lemma_id IN :lemmas AND i.page_id IN :pages",
			nativeQuery = true)
	List<IndexEntity> findByLemmasAndPages(@Param("lemmas") List<LemmaEntity> lemmas,
										   @Param("pages") List<PageEntity> pageg);
}
