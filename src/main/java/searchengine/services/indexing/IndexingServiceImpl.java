package searchengine.services.indexing;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.repositories.SiteRepository;
import searchengine.request.BadRequest;
import searchengine.request.OkResponse;

import java.util.*;
import java.util.concurrent.ExecutorService;

@Slf4j
@Setter
@Getter
@Service
@RequiredArgsConstructor

public class IndexingServiceImpl implements IndexingService {

    private final SchemaMake schemaActions;
    private final IndexingActions indexingActions;
    private Thread thread;
    public SiteRepository siteRepository;
    ExecutorService executorService;

    @Override
    public ResponseEntity<Object> indexingStart() {
        log.warn("метод startIndexing запущен");
        Set<SiteEntity> siteEntities = schemaActions.setSites();
        if (siteEntities.size() == 0)
            return new ResponseEntity<>(new BadRequest(false, "Пустой  список сайтов"),
                    HttpStatus.BAD_REQUEST);

        thread = new Thread(() -> indexingActions.startTreadsIndexing(siteEntities), "indexing-thread");
        thread.start();
        return new ResponseEntity<>(new OkResponse(true), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> indexingPageStart(String url) {

        log.warn("Mapping /indexPage executed");

        if (indexingActions.isIndexingActionsStarted())
            return new ResponseEntity<>(new BadRequest(false, "Индексация уже запущена"),
                    HttpStatus.BAD_REQUEST);

        if (url == null || url.equals(""))
            return new ResponseEntity<>(new BadRequest(false, "Индексацию запустить не удалось. Пустой поисковый запрос"),
                    HttpStatus.BAD_REQUEST);

        SiteEntity siteEntity = schemaActions.partialInit(url);
        if (siteEntity == null)
            return new ResponseEntity<>(new BadRequest(false, "Данная страница находится за пределами сайтов,указанных в конфигурационном файле"),
                    HttpStatus.BAD_REQUEST);
        thread = new Thread(() -> indexingActions.startPartialIndexing(siteEntity), "indexingActions-thread");
        thread.start();

        return new ResponseEntity<>(new OkResponse(true), HttpStatus.OK);

    }


    @Override
    public ResponseEntity<Object> indexingStop() {
        log.warn("Mapping /stopIndexing executed");

        if (!indexingActions.isIndexingActionsStarted())
            return new ResponseEntity<>(new BadRequest(false, "Индексация не запущена"),
                    HttpStatus.BAD_REQUEST);

        indexingActions.setOffOn(false);
        indexingActions.setIndexingActionsStarted(false);

        return new ResponseEntity<>(new OkResponse(true), HttpStatus.OK);
    }



}