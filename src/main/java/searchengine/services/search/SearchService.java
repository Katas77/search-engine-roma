package searchengine.services.search;

import org.springframework.http.ResponseEntity;

public interface SearchService {
    ResponseEntity<Object> search(String query, String url, int offset, int limit);


}
