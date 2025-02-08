package searchengine.utils.indexing;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import searchengine.model.Website;
import searchengine.model.Status;
import searchengine.model.Page;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexing.IndexingServiceImpl;
import searchengine.utils.lemma.LemmaTools;

import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Setter
@Getter
@Component
@RequiredArgsConstructor
public class IndexingTools {

    private boolean update = true;
    private static final int CORE_COUNT = Runtime.getRuntime().availableProcessors();
    private BlockingQueue<Page> blockingQueue = new LinkedBlockingQueue<>(100);
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private final LemmaTools lemmaService;
    private ForkJoinPool joinPool = new ForkJoinPool(CORE_COUNT);

    public void startThreadsIndexing(Website siteEntity) {
        log.warn("Full indexing will be started now");
        CountDownLatch latch = new CountDownLatch(2);
        logInfo(siteEntity);

        // Запускаем рекурсивную задачу для обработки страниц сайта
        RecursiveThreadBody(joinPool, siteEntity, latch);

        // Запускаем обработку лемм
        lemmasThreadBody(siteEntity, latch);

        try {
            latch.await(); // Ожидаем завершения обеих задач
        } catch (InterruptedException e) {
            log.error("InterruptedException occurred during await(): {}", e.getMessage());
        }

        joinPool.shutdownNow(); // Завершаем работу пула потоков

        if (update) {
            updateEntity(siteEntity); // Обновляем сущность сайта
        }
    }

    private void lemmasThreadBody(Website siteEntity, CountDownLatch latch) {
        lemmaService.setQueue(blockingQueue);
        lemmaService.setCycle(false);
        lemmaService.setSiteEntity(siteEntity);

        try {
            lemmaService.startCollecting();
        } catch (DataIntegrityViolationException | ConcurrentModificationException e) {
            log.error("Exception occurred during lemma processing: {}", e.getMessage());
        }

        latch.countDown();
        log.warn("Lemmas thread finished, latch = {}", latch.getCount());
    }

    private void RecursiveThreadBody(ForkJoinPool pool, Website siteEntity, CountDownLatch latch) {
        try {
            RecursiveMake action = new RecursiveMake(siteEntity.getUrl(), siteEntity, blockingQueue, pageRepository, siteEntity.getUrl());
            pool.invoke(action);
        } catch (NullPointerException | ConcurrentModificationException | RejectedExecutionException e) {
            log.error("Exception occurred during recursive task execution: {}", e.getMessage());
        }

        lemmaService.setCycle(true);
        latch.countDown();
        log.info("{} pages saved in DB.", pageRepository.countBySiteEntity(siteEntity));
        log.warn("Recursive thread finished, latch = {}", latch.getCount());
    }

    public void setIsActive(boolean value) {
        update = false;
        stopUpdate();

        try {
            log.warn("---stopped by user----");
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            log.error("InterruptedException occurred during sleep: {}", e.getMessage());
        } finally {
            joinPool.shutdownNow();
        }

        lemmaService.setOffOn(value);
        RecursiveMake.isActive = value;
    }

    private void updateEntity(Website siteEntity) {
        siteEntity.setStatus(Status.INDEXED);
        siteEntity.setLastError("");
        siteEntity.setStatusTime(LocalDateTime.now());
        int countPages = pageRepository.countBySiteEntity(siteEntity);
        setStatus(countPages, siteEntity);
        log.warn("Status of site {} set to {}, error set to {}",
                siteEntity.getName(),
                siteEntity.getStatus().toString(),
                siteEntity.getLastError());

        if (countPages == 1 && siteEntity.getUrl().equals(IndexingServiceImpl.oneUrl)) {
            siteEntity.setStatus(Status.INDEXED);
            siteEntity.setLastError("");
        }

        siteRepository.save(siteEntity);
        StringPool.clearAll();
    }

    private void logInfo(Website siteEntity) {
        log.info("{} with URL {} started indexing",
                siteEntity.getName(),
                siteEntity.getUrl());
        log.info("{} pages, {} lemmas, {} indexes in table",
                pageRepository.count(),
                lemmaRepository.count(),
                indexRepository.count());
    }

    public void stopUpdate() {
        List<Website> siteEntities = siteRepository.findAll();
        for (Website siteEntity : siteEntities) {
            if (siteEntity.getUrl().equals(IndexingServiceImpl.oneUrl) || isIndexing()) {
                continue;
            }

            int countPages = pageRepository.countBySiteEntity(siteEntity);
            if (countPages > 1) {
                siteEntity.setLastError("Индексирование было остановлено пользователем");
                siteEntity.setStatus(Status.FAILED);
            }

            setStatus(countPages, siteEntity);
            siteRepository.save(siteEntity);
        }
    }

    private void setStatus(int countPages, Website siteEntity) {
        switch (countPages) {
            case 0 -> {
                siteEntity.setStatus(Status.FAILED);
                siteEntity.setLastError("Ошибка индексирования: невозможно установить соединение с запрашиваемым веб-сайтом.");
            }
            case 1 -> {
                siteEntity.setStatus(Status.FAILED);
                siteEntity.setLastError("Ошибка индексирования: произошла длительная задержка при переходе по ссылкам страницы.");
            }
        }
    }

    private void stopIndexing(Website siteEntity) {
        int timeSleep = 8000;
        if (siteEntity.getUrl().contains("https://www.playback.ru")) {
            timeSleep = 11000;
        }

        while (true) {
            try {
                Thread.sleep(7000);
            } catch (InterruptedException e) {
                log.error("InterruptedException occurred during sleep: {}", e.getMessage());
            }

            if (!update) {
                return;
            }

            int countPages1 = pageRepository.countBySiteEntity(siteEntity);
            try {
                Thread.sleep(timeSleep);
            } catch (InterruptedException e) {
                log.error("InterruptedException occurred during sleep: {}", e.getMessage());
            }

            int countPages2 = pageRepository.countBySiteEntity(siteEntity);
            if (countPages1 == countPages2) {
                joinPool.shutdownNow();
                RecursiveMake.isActive = false;
                lemmaService.setOffOn(false);
                updateEntity(siteEntity);
                return;
            }
        }
    }

    private boolean isIndexing() {
        return siteRepository.existsByStatus(Status.INDEXED);
    }
}
