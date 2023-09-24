package searchengine.utils.searchandLemma;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
public class LemmaFinder {


    private static final String[] PARTICLES_NAMES = {"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private final LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
    private final LuceneMorphology luceneMorphology2 = new EnglishLuceneMorphology();

    public LemmaFinder() throws IOException {
    }

    public Map<String, Integer> collectLemmas(String text) {
        String[] words = arrayContainsRussianWords(text);
        HashMap<String, Integer> lemmas = new HashMap<>();


        for (String word : words) {
            if (word.isBlank() | ((word.length() == 1) && (!word.toLowerCase(Locale.ROOT).equals("я")))) {
                continue;
            }
            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }
            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }
            String normalWord = normalForms.get(0);

            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
            } else {
                lemmas.put(normalWord, 1);
            }
        }
        lemmas.putAll(collectLemmasEnglish(text));
        return lemmas;
    }

    public Map<String, Integer> collectLemmasEnglish(String text) {
        String[] words = arrayContainsEnglishWords(text);
        HashMap<String, Integer> lemmas = new HashMap<>();

        for (String word : words) {
            if (word.isBlank() | ((word.length() == 1))) {
                continue;
            }


            List<String> normalForms = luceneMorphology2.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }
            String normalWord = normalForms.get(0);

            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
            } else {
                lemmas.put(normalWord, 1);
            }
        }
        return lemmas;
    }


    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : PARTICLES_NAMES) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    private String[] arrayContainsEnglishWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^a-z\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

}
