package searchengine.utils.lemma;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.color.Colors;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.utils.searchandLemma.LemmaFinder;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Setter
@Service
@RequiredArgsConstructor
@Getter
public class LexicalReducerImpl implements LexicalReducer {
    private Lock lock = new ReentrantLock();
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
        while (true) {
            lock.lock();
            Page pageEntity =queue.poll(10, TimeUnit.SECONDS);//Если за 10 сек элемент так и не появился, метод вернет null.
            if ((pageEntity == null))
            {      log.info(Colors.ANSI_RED+"Останавливаю процесс индексирования. Пожалуйста, подождите около 20 секунд, пока появится сообщение о возможности осуществления поиска."+Colors.ANSI_RESET);
                break;}
                collectedLemmas = lemmaFinder.collectLemmas(cleanText(pageEntity.getContent()));
                collectedLemmas.values().removeIf(Objects::isNull);
                collectedLemmas.forEach((lemma, rank) -> {
                    Lemma lemmaEntity = createLemmaEntity(lemma, pageEntity.getSiteEntity());
                   Indexes index= new Indexes(pageEntity, lemmaEntity, rank);
                    indexEntities.add(index);
                    countIndexes++;
                });
        }
        saveDataToDatabase();
        log.warn(Colors.ANSI_PURPLE+ "Теперь можете осуществлять  поиск нужной информации"+Colors.ANSI_RESET);
        lock.unlock();
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

    private synchronized void saveDataToDatabase() {
        long startTime = System.currentTimeMillis();

        try {
            lemmaRepository.saveAll(lemmaEntities.values());
            indexRepository.saveAll(indexEntities);
        } finally {
            lemmaEntities.clear();
            indexEntities.clear();
        }

        log.info("Saved {} lemmas and {} indexes in {} ms",
                countLemmas, countIndexes, System.currentTimeMillis() - startTime);
    }


    public String cleanText(String input) {
        String cleaned = input.replaceAll("<[^>]*>", " ");
        cleaned = cleaned.replaceAll("[^а-яА-ЯёЁa-zA-Z\\s]", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned;
    }


}