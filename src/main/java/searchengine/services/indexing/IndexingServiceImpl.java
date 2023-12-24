package searchengine.services.indexing;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.forAll.GeneralRequest;
import searchengine.model.Website;
import searchengine.repositories.SiteRepository;
import searchengine.utils.indexing.WebsiteSaveInRepository;
import searchengine.utils.indexing.IndexingTools;

import java.util.*;


@Slf4j
@Setter
@Getter
@Service
@RequiredArgsConstructor

public class IndexingServiceImpl implements IndexingService {

    private final WebsiteSaveInRepository inRepository;
    private final IndexingTools tools;
    public final SiteRepository siteRepository;
    private final SitesList sitesList;
    public static String oneUrl = "";


    @Override
    public ResponseEntity<Object> indexingStart() {
        log.warn("--метод startIndexing запущен--");
        List<Thread> threadList = new ArrayList<>();
        List<Website> websiteList = inRepository.listSitesEntity();
        websiteList.forEach(siteEntity -> threadList.add(new Thread(() -> tools.startTreadsIndexing(siteEntity), "Thread - " + siteEntity.getName())));
        threadList.forEach(Thread::start);
        return new GeneralRequest().statusOk();
    }

    @Override
    public ResponseEntity<Object> indexingPageStart(String url) {
        log.warn("--метод indexingPageStart запущен--");
        oneUrl = url;
        if (!isConfigurations(url)) {
            oneUrl = "";
            return new GeneralRequest().indexPageFailed();
        } else
            return new GeneralRequest().statusOk();

    }

    @Override
    public ResponseEntity<Object> indexingStop() {
        log.warn("--stopIndexing --");
        tools.setIsActive(false);
        return new GeneralRequest().statusOk();
    }

    public boolean checkUrl(String url) {
        return url.matches("\"^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]\";");
    }

    public boolean isConfigurations(String url) {
        for (Site site : sitesList.getSites()) {
            if (url.startsWith(site.getUrl())) {
                return true;
            }
        }
        return false;
    }
}

