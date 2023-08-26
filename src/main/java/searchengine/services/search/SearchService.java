package searchengine.services.search;

import org.springframework.http.ResponseEntity;
import searchengine.dto.search.statistics.SearchData;


import java.util.List;

public interface SearchService {
    ResponseEntity<Object> search(String query, String url, int offset, int limit);


}
