package searchengine.services.indexing;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.forAll.Request;
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

    /**
     * Метод запуска индексации сайтов.
     *
     * @return ответ с результатом выполнения
     */
    @Override
    public ResponseEntity<Object> indexingStart() {
        log.info("--- Start indexing websites ---");

        List<Thread> threadList = new ArrayList<>();
        List<Website> websiteList = inRepository.listSitesEntity();

        for (Website siteEntity : websiteList) {
            Thread thread = new Thread(() -> tools.startThreadsIndexing(siteEntity),
                    "Thread - " + siteEntity.getName());
            threadList.add(thread);
            thread.start();
        }

        log.debug("Started {} threads for indexing.", threadList.size());
        return new Request().statusOk();
    }

    /**
     * Метод проверки конфигурации для индексации страницы.
     *
     * @param url URL страницы для индексации
     * @return ответ с результатом проверки
     */
    @Override
    public ResponseEntity<Object> indexingPageStart(String url) {
        log.info("--- Check configuration for indexing page: {} ---", url);

        if (!isConfigurationValid(url)) {
            log.error("Invalid configuration for URL: {}", url);
            return new Request().indexPageFailed();
        }

        log.info("Configuration valid for URL: {}. Starting indexing...", url);
        oneUrl = url;  // Сохраняем URL в статическом поле
        return new Request().statusOk();
    }

    /**
     * Метод остановки процесса индексации.
     *
     * @return ответ с результатом выполнения
     */
    @Override
    public ResponseEntity<Object> indexingStop() {
        log.info("--- Stopping indexing process ---");

        tools.setIsActive(false);
        return new Request().statusOk();
    }

    /**
     * Проверяет, является ли URL допустимым для индексации.
     *
     * @param url URL для проверки
     * @return true, если URL допустимый, иначе false
     */
    private boolean isConfigurationValid(String url) {
        for (Site site : sitesList.getSites()) {
            if (url.startsWith(site.getUrl())) {
                return true;
            }
        }
        return false;
    }
}