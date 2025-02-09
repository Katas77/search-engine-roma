package searchengine.services.indexing;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.response.DataTransmission;
import searchengine.model.Website;
import searchengine.repositories.SiteRepository;
import searchengine.utils.indexing.WebsiteSaveService;
import searchengine.utils.indexing.IndexingTools;

import java.util.*;

@Slf4j
@Setter
@Getter
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final WebsiteSaveService inRepository;
    private final IndexingTools tools;
    public final SiteRepository siteRepository;
    private final SitesList sitesList;
    public static String oneUrl = "";

    @Override
    public ResponseEntity<Object> indexingStart() {
        log.info("Start indexing websites");
        List<Thread> threadList = new ArrayList<>();
        List<Website> websiteList = inRepository.listSitesEntity();
        for (Website siteEntity : websiteList) {
            Thread thread = new Thread(() -> {
                try {
                    tools.startThreadsIndexing(siteEntity);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            },
                    "Thread - " + siteEntity.getName());
            threadList.add(thread);
            thread.start();
        }

        log.debug("Started {} threads for indexing.", threadList.size());
        return new DataTransmission().statusOk();
    }

    @Override
    public ResponseEntity<Object> indexingPageStart(String url) {
        log.info("--- Check configuration for indexing page: {} ---", url);

        if (!isConfigurationValid(url)) {
            log.error("Invalid configuration for URL: {}", url);
            return new DataTransmission().indexPageFailed();
        }

        log.info("Configuration valid for URL: {}. Starting indexing...", url);
        oneUrl = url;
        return new DataTransmission().statusOk();
    }

    @Override
    public ResponseEntity<Object> indexingStop() {
        log.info("Stopping indexing process");
        tools.setIsActive(false);
        return new DataTransmission().statusOk();
    }

    private boolean isConfigurationValid(String url) {
        for (Site site : sitesList.getSites()) {
            if (url.startsWith(site.getUrl())) {
                return true;
            }
        }
        return false;
    }

}