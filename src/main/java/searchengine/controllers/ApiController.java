package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import searchengine.model.IndexingStatus;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexing.IndexingService;
import searchengine.dto.search.SearchResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.services.search.SearchService;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.statistic.StatisticsService;
import searchengine.storage.BadRequest;

@Slf4j
@Setter
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

		if (isIndexing())
			return new ResponseEntity<>(new BadRequest(false, "Индексация уже запущена"),
					HttpStatus.BAD_REQUEST);

		return indexingService.indexingStart();
	}

	@PostMapping("/indexPage")
	public ResponseEntity<Object> indexPage(@RequestParam final String url) {
		return indexingService.indexingPageStart(url);
	}

	@GetMapping("/stopIndexing")
	public ResponseEntity<Object> stopIndexing() {
		return indexingService.indexingStop();
	}

	@GetMapping("/search")
	public ResponseEntity<SearchResponse> search(
			@RequestParam final String query,
			@RequestParam(required = false) final String site,
			@RequestParam final Integer offset,
			@RequestParam final Integer limit) {

		return ResponseEntity.ok(searchService.getSearchResults(query, site, offset, limit));
	}
	private boolean isIndexing() {
		return siteRepository.existsByStatus(IndexingStatus.INDEXING);
	}

}
