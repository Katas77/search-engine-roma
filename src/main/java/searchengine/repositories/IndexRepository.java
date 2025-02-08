package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Indexes;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Website;

import java.util.List;

@Transactional
@Repository
public interface IndexRepository extends JpaRepository<Indexes, Long> {
   // Integer countByPageEntity(Page pageEntity);

    @Query(value = "SELECT i.* FROM search_index i WHERE i.lemma_id IN :lemmas AND i.page_id IN :pages",
            nativeQuery = true)
    List<Indexes> findByLemmasAndPages(@Param("lemmas") List<Lemma> lemmas,@Param("pages") List<Page> pageg);
}
