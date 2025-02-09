package searchengine.utils.lemma;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.utils.searchandLemma.LemmaFinder;
import java.util.*;
import java.util.concurrent.BlockingQueue;

@Slf4j

@Setter
@Service
@RequiredArgsConstructor
@Getter
public class LemmasToolsImpl implements LemmaTools {
    @Setter
    private boolean offOn = true;
    private int countPages = 0;
    private int countLemmas = 0;
    private int countIndexes = 0;
    private Website siteEntity;
    private Indexes indexEntity;
    public volatile boolean cycle = false;
    private BlockingQueue<Page> queue;
    private Set<Indexes> indexEntities = new HashSet<>();
    private Map<String, Integer> collectedLemmas = new HashMap<>();
    private Map<String, Lemma> lemmaEntities = new HashMap<>();
    private final LemmaFinder lemmaFinder;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;

    public void startCollecting() {
        while (allowed()) {
            int countPag = pageRepository.countBySiteEntity(siteEntity);
            if (!offOn) {
                clearSaving();
                return;
            }
            Page pageEntity = queue.poll();
            if (pageEntity != null && countPag != 0) {
                collectedLemmas = lemmaFinder.collectLemmas(Jsoup.clean(pageEntity.getContent(), Safelist.simpleText()));
                collectedLemmas.values().removeIf(rank -> rank == null);
                collectedLemmas.forEach((lemma, rank) -> {
                    Lemma lemmaEntity = createLemmaEntity(lemma, pageEntity.getSiteEntity());
                    indexEntities.add(new Indexes(pageEntity, lemmaEntity, rank));
                    countIndexes++;
                });
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
        indexRepository.saveAll(indexEntities);
        sleeping(50, "sleeping after saving lemmas");
        log.warn("Saving index lasts - " + (System.currentTimeMillis() - idxSave) + " ms");
        indexEntities.clear();
    }

    private synchronized void savingLemmas() {
        long lemmaSave = System.currentTimeMillis();
        lemmaRepository.saveAll(lemmaEntities.values());
        sleeping(50, "sleeping after saving lemmas");
        log.warn("Saving lemmas lasts - " + (System.currentTimeMillis() - lemmaSave) + " ms");
        lemmaEntities.clear();
    }

    private String logAboutEachSite() {
        return countLemmas + " lemmas and " +
                countIndexes + " indexes saved " +
                "in DB from site with url " +
                Thread.currentThread().getName();
    }

    public Boolean allowed() {
        return !cycle || !queue.isEmpty();
    }

    @Override
    public boolean getCycle() {
        return false;
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