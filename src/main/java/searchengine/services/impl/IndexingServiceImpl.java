package searchengine.services.impl;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.storage.BadRequest;
import searchengine.storage.OkResponse;
import searchengine.tools.indexing.IndexingActions;
import searchengine.tools.indexing.SchemaActions;

import java.util.*;

@Slf4j
@Setter
@Getter
@Service
@RequiredArgsConstructor

public class IndexingServiceImpl implements IndexingService {

    private final SchemaActions schemaActions;
    private final IndexingActions indexingActions;
    private Thread thread;
    public SiteRepository siteRepository;


    @Override
    public ResponseEntity<Object> indexingStart() {

        log.warn("метод startIndexing запущен");

        Set<SiteEntity> siteEntities = schemaActions.setSites();
        if (siteEntities.size() == 0)
            return new ResponseEntity<>(new BadRequest(false, "Индексацию запустить не удалось. Пустой  список сайтов"),
                    HttpStatus.BAD_REQUEST);

        thread = new Thread(() -> indexingActions.startFullIndexing(siteEntities), "indexing-thread");
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

        setEnabled(false);
        indexingActions.setIndexingActionsStarted(false);

        return new ResponseEntity<>(new OkResponse(true), HttpStatus.OK);
    }

    public void setEnabled(boolean value) {
        indexingActions.setEnabled(value);
    }


}