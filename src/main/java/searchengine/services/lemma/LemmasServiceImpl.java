package searchengine.services.lemma;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.utils.searchandLemma.LemmaFinder;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;

import static java.lang.Thread.sleep;

@Slf4j
@Getter
@Setter
@Service
@RequiredArgsConstructor
public class LemmasServiceImpl implements LemmaService {
    private Boolean offOn = true;
    private Integer countPages = 0;
    private Integer countLemmas = 0;
    private Integer countIndexes = 0;
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
                collectedLemmas = lemmaFinder.collectLemmas
                        (Jsoup.clean(pageEntity.getContent(), Safelist.simpleText()));
                for (String lemma : collectedLemmas.keySet()) {
                    if (collectedLemmas.get(lemma) == null) {
                        continue;
                    }
                    int rank = collectedLemmas.get(lemma);
                    Lemma lemmaEntity = createLemmaEntity(lemma, pageEntity.getSiteEntity());
                    indexEntities.add(new Indexes(pageEntity, lemmaEntity, rank));
                    countIndexes++;
                }
            } else {
                sleeping(10, "Error sleeping while waiting for an item in line");
            }
        }
        savingIndexes();
        savingLemmas();

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

    private void savingIndexes() {
        long idxSave = System.currentTimeMillis();
        indexRepository.saveAll(indexEntities);
        sleeping(50, " sleeping after saving lemmas");
        log.warn("Saving index lasts -  " + (System.currentTimeMillis() - idxSave) + " ms");
        indexEntities.clear();
    }

    private void savingLemmas() {
        long lemmaSave = System.currentTimeMillis();
        try {
            lemmaRepository.saveAll(lemmaEntities.values());

        } catch (JpaSystemException exception) {
            System.out.println("JpaSystemException - "+exception.getMessage());
            List<String> lemmasL = new ArrayList<>();
            StringBuilder lemmaB = new StringBuilder();
            for (Map.Entry<String, Lemma> entry : lemmaEntities.entrySet()) {
                lemmaB.append(entry.getValue().getId() + " - " + entry.getValue().getLemma() + " - " + entry.getValue().getFrequency() + " - " + entry.getValue().getSiteEntity().toString() + " - ");
                lemmasL.add(String.valueOf(lemmaB));
                lemmaB = new StringBuilder();
            }
            try {
                Files.write(Paths.get("data/lemma.txt"), lemmasL);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }


        sleeping(50, "Error sleeping after saving lemmas");
        log.warn("Saving lemmas lasts - " + (System.currentTimeMillis() - lemmaSave) + " ms");
        lemmaEntities.clear();
    }

    private String logAboutEachSite() {
        return countLemmas + " lemmas and "
                + countIndexes + " indexes saved "
                + "in DB from site with url "
                + siteEntity.getUrl();
    }

    public Boolean allowed() {
        return !cycle | queue.iterator().hasNext();
    }

    public void setOffOn(boolean value) {
        offOn = value;
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
