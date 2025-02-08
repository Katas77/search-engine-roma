package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Website;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Transactional
@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    Optional<Page> findByPathAndSiteEntity(String path, Website siteEntity);
    @Query(value = "SELECT p.* FROM page p JOIN search_index i ON p.id = i.page_id WHERE i.lemma_id IN :lemmas",
            nativeQuery = true)
    List<Page> findByLemmas(@Param("lemmas") Collection<Lemma> lemmas);
    Integer countBySiteEntity(Website siteEntity);


}
