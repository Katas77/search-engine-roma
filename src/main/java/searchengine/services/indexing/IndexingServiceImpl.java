package searchengine.services.indexing;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.Website;
import searchengine.repositories.SiteRepository;
import searchengine.dto.forAll.BadRequest;
import searchengine.dto.forAll.OkResponse;
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
        websiteList.forEach(siteEntity -> threadList.add(new Thread(() -> tools.startTreadsIndexing(siteEntity),"Thread - "+siteEntity.getName())));
        threadList.forEach(Thread::start);
        return new ResponseEntity<>(new OkResponse(true), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> indexingPageStart(String url) {
        log.warn("--метод indexingPageStart запущен--");
        if (url == null || url.equals("") || checkUrl(url))
            return new ResponseEntity<>(new BadRequest(HttpStatus.BAD_REQUEST.value(), "Унифицированный указатель ресурса пустой, или невозможно определить определитель местонахождения запрашиваемого  ресурса"),
                    HttpStatus.BAD_REQUEST);
        if (isConfigurations(url)) {
            oneUrl = url;
            return new ResponseEntity<>(new OkResponse(true), HttpStatus.OK);
        } else
            return new ResponseEntity<>(new BadRequest(HttpStatus.BAD_REQUEST.value(), "Данная страница находится за пределами сайтов,указанных в конфигурационном файле"),
                    HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<Object> indexingStop() {
        log.warn("--stopIndexing --");
        tools.setIsActive(false);
        return new ResponseEntity<>(new OkResponse(true), HttpStatus.OK);
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

