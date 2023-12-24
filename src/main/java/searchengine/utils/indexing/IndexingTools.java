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
import searchengine.services.lemma.LemmaService;

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
    private ExecutorService executorService;
    private boolean update = true;
    private static final int CORE_COUNT = Runtime.getRuntime().availableProcessors();
    private BlockingQueue<Page> blockingQueue = new LinkedBlockingQueue<>(1_00);
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private final LemmaService lemmaService;
    private ForkJoinPool joinPool = new ForkJoinPool(CORE_COUNT);

    public void startTreadsIndexing(Website siteEntity) {
        log.warn("Full indexing will be started now");
        CountDownLatch latch = new CountDownLatch(2);
        logInfo(siteEntity);
        RecursiveThreadBody(joinPool, siteEntity, latch);
        Thread thread = new Thread(() -> indexed(siteEntity));
        thread.start();
        lemmasThreadBody(siteEntity, latch);
        try {
            latch.await();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
        joinPool.shutdownNow();
        if (update) {
            updateEntity(siteEntity);
        }
    }

    private void lemmasThreadBody(Website siteEntity, CountDownLatch latch) {
        lemmaService.setQueue(blockingQueue);
        lemmaService.setCycle(false);
        lemmaService.setSiteEntity(siteEntity);
        try {
            lemmaService.startCollecting();
        } catch (DataIntegrityViolationException | ConcurrentModificationException exception) {
        }
        latch.countDown();
        log.warn("thread finished, latch =  " + latch.getCount());
    }

    private void RecursiveThreadBody(@NotNull ForkJoinPool pool, Website siteEntity, CountDownLatch latch) {
        try {
            RecursiveMake action = new RecursiveMake(siteEntity.getUrl(), siteEntity, blockingQueue, pageRepository, siteEntity.getUrl());
            pool.invoke(action);
        } catch (NullPointerException | ConcurrentModificationException | RejectedExecutionException ignored) {
        }
        lemmaService.setCycle(true);
        latch.countDown();
        log.info(pageRepository.countBySiteEntity(siteEntity) + " pages saved in DB-");
        log.warn("thread finished, latch =  " + latch.getCount());
    }

    public void setIsActive(boolean value) {
        update = false;
        stopUpdate();
        try {
            log.warn("---остановлено пользователем----");
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
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
        log.warn("Status of site " + siteEntity.getName()
                + " set to " + siteEntity.getStatus().toString()
                + ", error set to " + siteEntity.getLastError());
        if (countPages == 1 & siteEntity.getUrl().equals(IndexingServiceImpl.oneUrl)) {
            siteEntity.setStatus(Status.INDEXED);
            siteEntity.setLastError("");
        }
        siteRepository.save(siteEntity);
        StringPool.clearAll();
    }

    private void logInfo(Website siteEntity) {
        log.info(siteEntity.getName() + " with URL " + siteEntity.getUrl() + " started indexing");
        log.info(pageRepository.count() + " pages, "
                + lemmaRepository.count() + " lemmas, "
                + indexRepository.count() + " indexes in table");
    }


    public void stopUpdate() {
        List<Website> siteEntities = siteRepository.findAll();
        for (Website siteEntity : siteEntities) {
            if (siteEntity.getUrl().equals(IndexingServiceImpl.oneUrl) | isIndexing()) {
                continue;
            }
            int countPages = pageRepository.countBySiteEntity(siteEntity);
            if (countPages > 1) {
                siteEntity.setLastError("Индексация остановлена пользователем");
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
                siteEntity.setLastError("Ошибка индексации: главная страница сайта не доступна");
            }
            case 1 -> {
                siteEntity.setStatus(Status.FAILED);
                siteEntity.setLastError("Ошибка индексации: сайт не доступен");
            }
        }
    }

    private void indexed(Website siteEntity) {
        int timeSleep = 6_000;
        if (siteEntity.getUrl().contains("https://www.playback.ru")) {
            timeSleep = 11_000;
        }
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
                Thread.sleep(timeSleep);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
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


