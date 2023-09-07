package searchengine.services.indexing;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.repositories.SiteRepository;
import searchengine.dto.forAll.BadRequest;
import searchengine.dto.forAll.OkResponse;
import searchengine.utils.indexing.EntityMake;
import searchengine.utils.indexing.IndexingTools;

import java.util.*;


@Slf4j
@Setter
@Getter
@Service
@RequiredArgsConstructor

public class IndexingServiceImpl implements IndexingService {
    private final EntityMake entityMake;
    private final IndexingTools tools;
    public final SiteRepository siteRepository;
    private final SitesList sitesList;


    @Override
    public ResponseEntity<Object> indexingStart() {
        log.warn("--метод startIndexing запущен--");
        List<Thread> threadList = new ArrayList<>();
        entityMake.listSitesEntity().forEach(siteEntity -> threadList.add(new Thread(() -> tools.startTreadsIndexing(siteEntity))));
        threadList.forEach(Thread::start);
        return new ResponseEntity<>(new OkResponse(true), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> indexingPageStart(String url) {
        log.warn("--метод indexingPageStart запущен--");
        if (url == null || url.equals(""))
            return new ResponseEntity<>(new BadRequest(false, "Унифицированный указатель ресурса пустой"),
                    HttpStatus.BAD_REQUEST);
        if (checkUrl(url))
            return new ResponseEntity<>(new BadRequest(false, "Данная страница находится за пределами сайтов,указанных в конфигурационном файле"),
                    HttpStatus.BAD_REQUEST);



        return new ResponseEntity<>(new OkResponse(true), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> indexingStop() {
        log.warn("--stopIndexing --");
        tools.setIsActive(false);
        tools.setIndexingStarted(false);
        return new ResponseEntity<>(new OkResponse(true), HttpStatus.OK);
    }

    public boolean checkUrl(String url) {
        return url.matches("\"^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]\";");
    }
}

