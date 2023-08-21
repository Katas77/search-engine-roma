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

@Slf4j
@Setter
@Getter
@Service
@RequiredArgsConstructor

public class IndexingServiceImpl implements IndexingService {

    private final TablesMake schemaMake;
    private final IndexingOperations indexingOperations;
    public final SiteRepository siteRepository;


    @Override
    public ResponseEntity<Object> indexingStart() {
        log.warn("метод startIndexing запущен");
        Set<SiteEntity> siteEntities = schemaMake.setSites();
        if (siteEntities.size() == 0)
            return new ResponseEntity<>(new BadRequest(false, "Пустой  список сайтов"),
                    HttpStatus.BAD_REQUEST);
        Thread thread = new Thread(() -> indexingOperations.startTreadsIndexing(siteEntities));
        thread.start();
        return new ResponseEntity<>(new OkResponse(true), HttpStatus.OK);
    }
    @Override
    public ResponseEntity<Object> indexingPageStart(String url) {
        log.warn("Mapping /indexPage executed");
        if (indexingOperations.isIndexingActionsStarted())
            return new ResponseEntity<>(new BadRequest(false, "Индексация уже запущена"),
                    HttpStatus.BAD_REQUEST);
        if (url == null || url.equals(""))
            return new ResponseEntity<>(new BadRequest(false, "Индексацию запустить не удалось. Пустой поисковый запрос"),
                    HttpStatus.BAD_REQUEST);
        SiteEntity siteEntity = schemaMake.partialInit(url);
        if (siteEntity == null)
            return new ResponseEntity<>(new BadRequest(false, "Данная страница находится за пределами сайтов,указанных в конфигурационном файле"),
                    HttpStatus.BAD_REQUEST);
        Thread thread = new Thread(() -> indexingOperations.startPartialIndexing(siteEntity), "indexingActions-thread");
        thread.start();
        return new ResponseEntity<>(new OkResponse(true), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> indexingStop() {
        log.warn("Mapping /stopIndexing executed");
        if (!indexingOperations.isIndexingActionsStarted())
            return new ResponseEntity<>(new BadRequest(false, "Индексация не запущена"),
                    HttpStatus.BAD_REQUEST);
       indexingOperations.setOffOn(false);
       indexingOperations.setIndexingActionsStarted(false);
        return new ResponseEntity<>(new OkResponse(true), HttpStatus.OK);
    }


}