package searchengine.utils.searchandLemma;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class LemmaFinder {
    private static final String[] PARTICLES_NAMES = {"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private final LuceneMorphology luceneMorphologyRu = new RussianLuceneMorphology();
    private final LuceneMorphology luceneMorphologyEn = new EnglishLuceneMorphology();

    public LemmaFinder() throws IOException {
        // Конструктор для инициализации морфологий
    }

    public Map<String, Integer> collectLemmas(String text) {
        Map<String, Integer> lemmas = new HashMap<>();

        lemmas.putAll(collectLemmasFromLanguage(text, luceneMorphologyRu, true));
        lemmas.putAll(collectLemmasFromLanguage(text, luceneMorphologyEn, false));

        return lemmas;
    }

    private Map<String, Integer> collectLemmasFromLanguage(String text, LuceneMorphology morphology, boolean isRussian) {
        String[] words = isRussian ? extractRussianWords(text) : extractEnglishWords(text);
        Map<String, Integer> lemmas = new HashMap<>();

        for (String word : words) {
            if (isValidWord(word, isRussian)) {
                List<String> normalForms = morphology.getNormalForms(word);
                if (!normalForms.isEmpty()) {
                    String normalWord = normalForms.get(0);
                    lemmas.put(normalWord, lemmas.getOrDefault(normalWord, 0) + 1);
                }
            }
        }
        return lemmas;
    }

    private boolean isValidWord(String word, boolean isRussian) {
        return !word.isBlank() && !(isRussian && word.length() == 1 && !word.equalsIgnoreCase("я"));
    }

    private String[] extractEnglishWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^a-z\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private String[] extractRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }
}
