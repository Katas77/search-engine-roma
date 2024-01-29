package searchengine.utils.searchandLemma;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.english.EnglishMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class LemmaSearchTools {
    private static EnglishMorphology englishMorphology;

    static {
        try {
            englishMorphology = new EnglishMorphology();
        } catch (IOException ignored) {
            System.out.println("Continue code as normal");
        }
    }

    private static RussianLuceneMorphology russianLuceneMorphology;

    static {
        try {
            russianLuceneMorphology = new RussianLuceneMorphology();
        } catch (IOException ignored) {
            System.out.println("Continue code as normal");
        }
    }

    public List<String> getLemma(String word) {
        List<String> lemmaList = new ArrayList<>();
        try {
            if (isRussianWord(word)) {
                List<String> lemmaForms = russianLuceneMorphology.getNormalForms(word);
                if (!isServiceWord(word) && !word.isEmpty()) {
                    lemmaList.addAll(lemmaForms);
                }
            }
        } catch (Exception ignored) {
        }
        try {
            if (!isRussianWord(word)) {
                List<String> lemmaForms = englishMorphology.getNormalForms(word);
                lemmaList.addAll(lemmaForms);
            }
        } catch (Exception ignored) {
        }
        return lemmaList;
    }

    public List<Integer> findLemmaIndexInText(String text, String lemma) {
        List<Integer> lemmaIndexList = new ArrayList<>();
        String[] elements = text.toLowerCase(Locale.ROOT).split("\\p{Punct}|\\s");
        int index = 0;
        for (String element : elements) {
            List<String> lemmas = getLemma(element);
            for (String lem : lemmas) {
                if (lem.equals(lemma)) {
                    lemmaIndexList.add(index);
                }
            }
            index += element.length() + 1;
        }
        return lemmaIndexList;
    }

    public static boolean isRussianWord(String word) {
        int length = word.replaceAll("[a-zA-Z0-9]+", "").trim().length();
        return length != 0;
    }

    private boolean isServiceWord(String word) {
        List<String> morphForm = russianLuceneMorphology.getMorphInfo(word);
        for (String element : morphForm) {
            if (element.contains("ПРЕДЛ")
                    || element.contains("СОЮЗ")
                    || element.contains("МЕЖД")
                    || element.contains("МС")
                    || element.contains("ЧАСТ")
                    || element.length() <= 3) {
                return true;
            }
        }
        return false;
    }

    public String removeHtmlTags(String html) {
        Pattern pattern = Pattern.compile("<[^>]*>");
        Matcher matcher = pattern.matcher(html);
        return matcher.replaceAll("");
    }
}
