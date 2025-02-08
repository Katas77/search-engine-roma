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

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

@Slf4j
@Getter
@Setter
@Service
@RequiredArgsConstructor
public class LemmasToolsImpl implements LemmaTools {
    @Setter
    private boolean offOn = true;
    private int countPages = 0;
    private int countLemmas = 0;
    private int countIndexes = 0;
    private Website siteEntity;
    private Indexes indexEntity;
    private volatile boolean cycle = false;
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
            if (pageEntity != null & countPag != 0) {
                this.collectedLemmas = lemmaFinder.collectLemmas(Jsoup.clean(pageEntity.getContent(), Safelist.simpleText()));
                for (String lemma : this.collectedLemmas.keySet()) {
                    if (this.collectedLemmas.get(lemma) == null) {
                        this.collectedLemmas.remove(lemma);
                    }
                }
                for (String lemma : this.collectedLemmas.keySet()) {
                    if (this.collectedLemmas.get(lemma) != null) {
                        int rank = this.collectedLemmas.get(lemma);
                        Lemma lemmaEntity = createLemmaEntity(lemma, pageEntity.getSiteEntity());
                        this.indexEntities.add(new Indexes(pageEntity, lemmaEntity, rank));
                        countIndexes++;
                    } else {
                        log.error("collectedLemmas.get(lemma) must not be null");
                    }
                }
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
        this.indexRepository.saveAll(indexEntities);
        sleeping(50, "sleeping after saving lemmas");
        log.warn("Saving index lasts - " + (System.currentTimeMillis() - idxSave) + " ms");
        this.indexEntities.clear();
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
                currentThread().getName();
    }

    public Boolean allowed() {
        return !cycle | queue.iterator().hasNext();
    }

    private static void sleeping(int millis, String s) {
        try {
            sleep(millis);
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