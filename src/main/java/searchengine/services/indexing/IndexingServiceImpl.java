package searchengine.services.indexing;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.repositories.SiteRepository;
import searchengine.dto.forAll.BadRequest;
import searchengine.dto.forAll.OkResponse;
import java.util.*;

@Slf4j
@Setter
@Getter
@Service
@RequiredArgsConstructor

public class IndexingServiceImpl implements IndexingService {

    private final TablesMake tableMake;
    private final IndexingOperations indexingOperations;
    public final SiteRepository siteRepository;


    @Override
    public ResponseEntity<Object> indexingStart() {
        log.warn("метод startIndexing запущен");
        Set<SiteEntity> siteEntities = tableMake.setSites();
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
        if (url == null || url.equals(""))
            return new ResponseEntity<>(new BadRequest(false, "Индексацию запустить не удалось. Пустой поисковый запрос"),
                    HttpStatus.BAD_REQUEST);
        SiteEntity siteEntity = tableMake.oneSiteEntity(url);

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
       indexingOperations.setIsActive(false);
       indexingOperations.setIndexingStarted(false);
        return new ResponseEntity<>(new OkResponse(true), HttpStatus.OK);
    }


}