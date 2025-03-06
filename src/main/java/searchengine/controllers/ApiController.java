package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import searchengine.dto.searh.SearchData;
import searchengine.dto.searh.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.response.DtoMessenger;
import searchengine.model.Status;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexing.IndexingService;
import searchengine.services.search.SearchService;
import searchengine.services.statistic.StatisticsService;

import java.util.ArrayList;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final SearchService searchService;
    private final IndexingService indexingService;
    private final StatisticsService statisticsService;

    private final ArrayList<SearchData> searchData = new ArrayList<>();

    private final SiteRepository siteRepository;
    private final IndexRepository indexRepository;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> getStatistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing() {
        if (isIndexing()) {
            indexingService.indexingStop();
            return new DtoMessenger().indexationAlreadyStarted();
        }
        return indexingService.indexingStart();
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Object> indexPage(
            @RequestParam(defaultValue = "https://www.playback.ru/product/1124022.html") final String url) {
        return indexingService.indexingPageStart(url);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        if (!isIndexing()) {
            return new DtoMessenger().indexingNotRunning();
        }
        return indexingService.indexingStop();
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(
            @RequestParam(required = false, defaultValue = "смартфон") String query,
            @RequestParam(required = false, defaultValue = "") String site,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "10") int limit) {

        if (isIndexRepositoryEmpty()) {
            searchData.add(new SearchData("-", "", "",
                    "Данные еще не внесены в таблицу «search_index», повторите запрос через 20 секунд",
                    "", 1));

            return new ResponseEntity<>(new SearchResponse(true, 1, searchData), HttpStatus.OK);
        }
        return searchService.search(query, site, offset, limit);
    }

    private boolean isIndexing() {
        return siteRepository.existsByStatus(Status.INDEXING);
    }

    private boolean isIndexRepositoryEmpty() {
        return indexRepository.findAll().size() < 25;
    }
}
