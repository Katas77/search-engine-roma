package searchengine.utils.lemma;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import searchengine.color.Colors;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.utils.searchandLemma.LemmaFinder;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j

@Setter
@Service
@RequiredArgsConstructor
@Getter
public class LemmasToolsImpl implements LemmaTools {
    private Lock lock = new ReentrantLock();
    private boolean offOn = true;
    private int countPages = 0;
    private int countLemmas = 0;
    private int countIndexes = 0;
    private Website siteEntity;
    private Indexes indexEntity;
    private BlockingQueue<Page> queue;
    private Set<Indexes> indexEntities = new HashSet<>();
    private Map<String, Integer> collectedLemmas = new HashMap<>();
    private Map<String, Lemma> lemmaEntities = new HashMap<>();
    private final LemmaFinder lemmaFinder;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;

    public void startCollecting() throws InterruptedException {
        while (allowed()) {
            int countPag = pageRepository.countBySiteEntity(siteEntity);
            if (!offOn) {
                clearSaving();
                return;
            }
            lock.lock();
            Page pageEntity = queue.poll();
            if (pageEntity == null) {
                continue;
            }
            if (countPag != 0) {
                collectedLemmas = lemmaFinder.collectLemmas(Jsoup.clean(pageEntity.getContent(), Safelist.simpleText()));
                collectedLemmas.values().removeIf(Objects::isNull);
                collectedLemmas.forEach((lemma, rank) -> {
                    Lemma lemmaEntity = createLemmaEntity(lemma, pageEntity.getSiteEntity());
                   Indexes index= new Indexes(pageEntity, lemmaEntity, rank);
                    indexEntities.add(index);
                    countIndexes++;
                    log.info(Colors.ANSI_BLUE+"Adding index to collection: {}"+Colors.ANSI_RESET, index);
                    log.info("Current size of indexEntities: {}", indexEntities.size());
                });
                lock.unlock();
            } else {
                sleeping(10, "Error sleeping while waiting for an item in line");
            }
        }
        savingLemmas();
        savingIndexes();
        log.warn(logAboutEachSite());
    }

    public Lemma createLemmaEntity(String lemma, Website siteEntity) {
        Lemma lemmaObj;
        if (lemmaEntities.containsKey(lemma)) {
            int oldFreq = lemmaEntities.get(lemma).getFrequency();
            lemmaEntities.get(lemma).setFrequency(oldFreq + 1);
            lemmaObj = lemmaEntities.get(lemma);
        } else {
            lemmaObj = new Lemma(siteEntity, lemma, 1);
            lemmaEntities.put(lemma, lemmaObj);
            countLemmas++;
        }
        return lemmaObj;
    }

    private synchronized void savingIndexes() {
        long idxSave = System.currentTimeMillis();
        try {
            indexRepository.saveAll(indexEntities);
        } finally {
            indexEntities.clear();
        }
        log.warn("Saving index lasts - " + (System.currentTimeMillis() - idxSave) + " ms");
    }

    private synchronized void savingLemmas() {
        try {
            lemmaRepository.saveAll(lemmaEntities.values());
        } finally {
            lemmaEntities.clear();
        }
        long lemmaSave = System.currentTimeMillis();
        sleeping(50, "sleeping after saving lemmas");
        log.warn("Saving lemmas lasts - " + (System.currentTimeMillis() - lemmaSave) + " ms");

    }

    private String logAboutEachSite() {
        return Colors.ANSI_PURPLE+countLemmas + " lemmas and " +
                countIndexes + " indexes saved " +
                "in DB from site with url "+Colors.ANSI_RESET;
    }

    public Boolean allowed() throws InterruptedException {
        Thread.sleep(10);
        return !queue.isEmpty();
    }


    private void sleeping(int millis, String s) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.error(s);
        }
    }

    private void clearSaving() {
        queue.clear();
        savingLemmas();
        savingIndexes();
        log.warn(logAboutEachSite());
    }
}