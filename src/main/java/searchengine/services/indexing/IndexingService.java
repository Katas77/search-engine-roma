package searchengine.services.indexing;

import org.springframework.http.ResponseEntity;

public interface IndexingService {

    ResponseEntity<Object> indexingStop();

    ResponseEntity<Object> indexingStart();

    ResponseEntity<Object> indexingPageStart(String url);
}
