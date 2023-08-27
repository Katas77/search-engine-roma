package searchengine.controllers;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.Status;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexing.IndexingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import searchengine.services.search.SearchService;
import searchengine.request.BadRequest;
import searchengine.services.statistic.StatisticsService;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor

public class ApiController {

    private final SearchService searchService;
    private final IndexingService indexingService;
    private final StatisticsService statisticsService;


    @Autowired
    SiteRepository siteRepository;


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing() {
        if (isIndexing()) {
            indexingService.indexingStop();
            return new ResponseEntity<>(new BadRequest(false, "Индексация уже запущена"),
                    HttpStatus.BAD_REQUEST);
        }
        return indexingService.indexingStart();
    }

   @PostMapping("/indexPage")
    public ResponseEntity<Object> indexPage(@RequestParam final String url) {
        return indexingService.indexingPageStart(url);}


    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        return indexingService.indexingStop();
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(@RequestParam(defaultValue = "купить") String query,
                                         @RequestParam(required = false, defaultValue = "") String site,
                                         @RequestParam(required = false) int offset,
                                         @RequestParam(required = false) int limit) {

        return searchService.search(query, site, offset, limit);
    }

    private boolean isIndexing() {
        return siteRepository.existsByStatus(Status.INDEXING);
    }

}
