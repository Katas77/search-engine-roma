package searchengine.utils.indexing;

import lombok.Getter;
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
import searchengine.utils.lemma.LexicalReducer;
import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.*;

@Setter
@Getter
@Component
@Slf4j
public class IndexerKit {

    private boolean update = true;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private final LexicalReducer lemmaService;

    public IndexerKit(PageRepository pageRepository,
                      LemmaRepository lemmaRepository,
                      IndexRepository indexRepository,
                      SiteRepository siteRepository,
                      LexicalReducer lemmaService) {
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.siteRepository = siteRepository;
        this.lemmaService = lemmaService;

        int coreCount = Runtime.getRuntime().availableProcessors();
        this.joinPool = new ForkJoinPool(coreCount);
    }

    private ForkJoinPool joinPool;
    private BlockingQueue<Page> blockingQueue = new LinkedBlockingQueue<>(100);

    public void startThreadsIndexing(Website siteEntity) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);//будет ждать завершения двух операций перед тем, как позволить основному потоку продолжить выполнение.
        logInfo(siteEntity);
        Runnable runnableLemmas = () -> {
            try {
                lemmasThreadBody(siteEntity, latch);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        Runnable runnableRecursive = () -> {
            RecursiveThreadBody(joinPool, siteEntity, latch);

        };
        new Thread(runnableLemmas).start();
        new Thread(runnableRecursive).start();
        Thread thread = new Thread(() -> indexedIsStopped(siteEntity));
        thread.start();
        try {
            latch.await();//await(), блокируется до тех пор, пока все операции не завершатся и счётчик не достигнет нуля
        } catch (InterruptedException e) {
            log.error("InterruptedException occurred during await(): {}", e.toString());
        }
        joinPool.shutdown();
        joinPool.awaitTermination(10, TimeUnit.SECONDS);
        if (update) {
            updateEntity(siteEntity);
        }

    }

    private void lemmasThreadBody(Website siteEntity, CountDownLatch latch) throws InterruptedException {
        lemmaService.setQueue(blockingQueue);
        lemmaService.setSiteEntity(siteEntity);
        try {
            lemmaService.startCollecting();
        } catch (DataIntegrityViolationException | ConcurrentModificationException e) {
            log.error("Exception occurred during lemma processing: {}", e.toString());
        }
        latch.countDown();//метод countDown() метод уменьшает значение счётчика на единицу.
        log.warn("Lemmas thread finished, latch = {}", latch.getCount());
    }

    private void RecursiveThreadBody(ForkJoinPool pool, Website siteEntity, CountDownLatch latch) {
        try {
            SubtaskDispatcher action = new SubtaskDispatcher(siteEntity.getUrl(), siteEntity, blockingQueue, pageRepository, siteEntity.getUrl());
            pool.invoke(action);
        } catch (Exception e) {
            log.error("Exception occurred during recursive task execution: {}", e.toString());
        }
        latch.countDown();
        log.info("{} pages saved in DB.", pageRepository.countBySiteEntity(siteEntity));
        log.warn("Recursive thread finished, latch = {}", latch.getCount());
    }

    public void setIsActive(boolean value) {
        update = false;
        stopUpdate();
        try {
            log.warn("stopped by user");
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            log.error("InterruptedException occurred during sleep: {}", e.toString());
        } finally {
            joinPool.shutdownNow();
        }
        SubtaskDispatcher.isActive = value;
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

    private void indexedIsStopped(Website siteEntity) {
        while (true) {
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
            if (!update) {
                return;
            }
            int countPages1 = pageRepository.countBySiteEntity(siteEntity);
            try {
                Thread.sleep(9_000);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
            int countPages2 = pageRepository.countBySiteEntity(siteEntity);
            if (countPages1 == countPages2) {
                joinPool.shutdownNow();
                SubtaskDispatcher.isActive = false;
                updateEntity(siteEntity);
                return;
            }
        }
    }

    private boolean isIndexing() {
        return siteRepository.existsByStatus(Status.INDEXED);
    }
}
