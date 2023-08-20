package searchengine.services.indexing;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.Status;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.tools.StringPool;
import searchengine.tools.UrlFormatter;
import searchengine.services.lemma.LemmaService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Setter
@Getter
@Component
@RequiredArgsConstructor
public class IndexingOperationsImpl implements IndexingOperations {

    private final String[] errors = {
            "Ошибка индексации: главная страница сайта не доступна",
            "Ошибка индексации: сайт не доступен",
            ""};

    private Boolean offOn = true;
    private SiteEntity siteEntity;
    private BlockingQueue<PageEntity> queueOfPagesForLemmasCollecting = new LinkedBlockingQueue<>(1_000);
    private RecursiveMake action;
    private boolean indexingActionsStarted = false;
    private final SitesList sitesList;
    private final Environment environment;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private final LemmaService lemmasAndIndexCollectingService;

    @Override
    public void startTreadsIndexing(Set<SiteEntity> siteEntities) {
        log.warn("Full indexing will be started now");
        ForkJoinPool pool = new ForkJoinPool();
        setIndexingActionsStarted(true);
        for (SiteEntity siteEntity : siteEntities) {
            if (!offOn) {
                stopPressedActions(pool);
                break;
            }
            CountDownLatch latch = new CountDownLatch(2);
            writeLogBeforeIndexing(siteEntity);

            Thread scrapingThread = new Thread(() -> crawlThreadBody(pool, siteEntity, latch), "crawl-thread");
            Thread lemmasCollectorThread = new Thread(() -> lemmasThreadBody(siteEntity, latch), "lemmas-thread");

            scrapingThread.start();
            lemmasCollectorThread.start();

            awaitLatch(latch);
            doActionsAfterIndexing(siteEntity);

        }
        pool.shutdownNow();

        setIndexingActionsStarted(false);
    }

    private void lemmasThreadBody(SiteEntity siteEntity, CountDownLatch latch) {
        lemmasCollectingActions(siteEntity);
        latch.countDown();
        log.warn("lemmas-finding-thread finished, latch =  " + latch.getCount());
    }

    private void crawlThreadBody(@NotNull ForkJoinPool pool, SiteEntity siteEntity, @NotNull CountDownLatch latch) {
        action = new RecursiveMake(siteEntity.getUrl(), siteEntity, queueOfPagesForLemmasCollecting, environment, pageRepository, getHomeSiteUrl(siteEntity.getUrl()), siteEntity.getUrl());
        pool.invoke(action);

        latch.countDown();
        lemmasAndIndexCollectingService.setScrapingIsDone(true);

        log.info(pageRepository.countBySiteEntity(siteEntity) + " pages saved in DB");
        log.warn("crawl-thread finished, latch =  " + latch.getCount());
    }

    @Override
    public void startPartialIndexing(SiteEntity siteEntity) {
        log.warn("Partial indexing will be started now");
        Set<SiteEntity> oneEntitySet = new HashSet<>();
        oneEntitySet.add(siteEntity);
        startTreadsIndexing(oneEntitySet);
    }

    private void lemmasCollectingActions(SiteEntity siteEntity) {
        lemmasAndIndexCollectingService.setIncomeQueue(queueOfPagesForLemmasCollecting);
        lemmasAndIndexCollectingService.setScrapingIsDone(false);
        lemmasAndIndexCollectingService.setSiteEntity(siteEntity);
        lemmasAndIndexCollectingService.startCollecting();
    }



    private void stopPressedActions(ForkJoinPool pool) {

        try {
            log.warn("STOP pressed");
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        } finally {
            pool.shutdownNow();
            setOffOn(true);
            setIndexingActionsStarted(false);

        }
    }

    @Override
    public boolean isIndexingActionsStarted() {
        return indexingActionsStarted;
    }

    @Override
    public void setOffOn(boolean value) {
        offOn = value;
        lemmasAndIndexCollectingService.setEnabled(value);
        RecursiveMake.offOn = value;
    }

    private void doActionsAfterIndexing(@NotNull SiteEntity siteEntity) {
        siteEntity.setStatus(Status.INDEXED);
        siteEntity.setLastError("");
        siteEntity.setStatusTime(LocalDateTime.now());
        int countPages = pageRepository.countBySiteEntity(siteEntity);
        switch (countPages) {
            case 0 -> {
                siteEntity.setStatus(Status.FAILED);
                siteEntity.setLastError(errors[0]);
            }
            case 1 -> {
                siteEntity.setStatus(Status.FAILED);
                siteEntity.setLastError(errors[1]);
            }
        }
        if (offOn) {
            log.warn("Status of site " + siteEntity.getName()
                    + " set to " + siteEntity.getStatus().toString()
                    + ", error set to " + siteEntity.getLastError());
        } else {
            siteEntity.setLastError("Индексация остановлена пользователем");
            siteEntity.setStatus(Status.FAILED);
            log.warn("Status of site " + siteEntity.getName()
                    + " set to " + siteEntity.getStatus().toString()
                    + ", error set to " + siteEntity.getLastError());
        }

        siteEntity.setUrl(getHomeSiteUrl(siteEntity.getUrl()));
        siteRepository.save(siteEntity);
        StringPool.clearAll();
    }

    private String getHomeSiteUrl(String url){
        String result = null;
        for (Site s: sitesList.getSites()) {
            if (s.getUrl().startsWith(UrlFormatter.getShortUrl(url))){
                result = s.getUrl();
                break;
            }
        }
        return result;
    }


    private void awaitLatch(@NotNull CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("Can't await latch");
        }
    }

    private void writeLogBeforeIndexing( SiteEntity siteEntity) {
        log.info(siteEntity.getName() + " with URL " + siteEntity.getUrl() + " started indexing");
        log.info(pageRepository.count() + " pages, "
                + lemmaRepository.count() + " lemmas, "
                + indexRepository.count() + " indexes in table");
    }
}
