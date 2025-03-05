package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.searh.SearchData;
import searchengine.dto.searh.SearchResponse;
import searchengine.model.Indexes;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Website;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.indexing.JsoupConnect;
import searchengine.utils.searchandLemma.LemmaSearchTools;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaSearchTools lemmaFinderUtil;
    private final JsoupConnect jsoupConnects;

    @Override
    public ResponseEntity<Object> search(String query, String url, int offset, int limit) {
        if (query == null || query.isBlank()) {
            return new ResponseEntity<>(new SearchResponse(false, 0, Collections.emptyList()), HttpStatus.BAD_REQUEST);
        }

        List<SearchData> searchData = url.isEmpty()
                ? searchThroughAllSites(query)
                : onePageSearch(query, url);
        if (searchData.isEmpty()) {
            return new ResponseEntity<>(new SearchResponse(true, 0, Collections.emptyList()), HttpStatus.NOT_FOUND);
        }
        logSearchResults(query, searchData);
        return new ResponseEntity<>(new SearchResponse(true, searchData.size(), searchDataOffset(searchData, offset, limit)), HttpStatus.OK);
    }


    private void logSearchResults(String query, List<SearchData> searchData) {
        for (SearchData data : searchData) {
            String uri = data.getUri();
            String output = uri.startsWith("https:") ? uri : data.getSite() + uri;
            log.info("'{}' - найден: {}", query, output);
        }
    }

    public List<SearchData> searchThroughAllSites(String query) {
        List<Lemma> sortedLemmas = getSortedLemmas(query);
        List<SearchData> searchData = getSearchDataList(sortedLemmas, getQueryIntoLemma(query));
        searchData.sort(Comparator.comparing(SearchData::getRelevance).reversed());
        return searchData;
    }

    public List<SearchData> onePageSearch(String query, String url) {
        Website siteEntity = siteRepository.findByUrl(url).orElseThrow();
        List<String> lemmasFromQuery = getQueryIntoLemma(query);
        List<Lemma> lemmasFromSite = getLemmasFromSite(lemmasFromQuery, siteEntity);
        return getSearchDataList(lemmasFromSite, lemmasFromQuery);
    }

    private List<String> getQueryIntoLemma(String query) {
        String[] words = query.toLowerCase(Locale.ROOT).split(" ");
        return Arrays.stream(words)
                .flatMap(word -> lemmaFinderUtil.getLemma(word).stream())
                .collect(Collectors.toList());
    }

    private List<Lemma> getSortedLemmas(String query) {
        List<Website> sites = siteRepository.findAll();
        List<Lemma> sortedLemmas = new ArrayList<>();

        List<String> lemmasFromQuery = getQueryIntoLemma(query);
        for (Website site : sites) {
            sortedLemmas.addAll(getLemmasFromSite(lemmasFromQuery, site));
        }
        return sortedLemmas;
    }

    private List<Lemma> getLemmasFromSite(List<String> lemmas, Website site) {
        List<Lemma> lemmaList = lemmaRepository.findLemmasBySite(lemmas, site);
        lemmaList.sort(Comparator.comparingInt(Lemma::getFrequency));
        return lemmaList;
    }

    private List<SearchData> getSearchDataList(List<Lemma> lemmas, List<String> lemmasFromQuery) {
        if (lemmas.isEmpty() || lemmasFromQuery.isEmpty()) {
            return Collections.emptyList();
        }
        List<Page> sortedPageList = pageRepository.findByLemmas(lemmas);
        List<Indexes> sortedIndexList = indexRepository.findByLemmasAndPages(lemmas, sortedPageList);
        LinkedHashMap<Page, Float> sortedPagesByAbsRelevance = getSortPagesWithAbsRelevance(sortedPageList, sortedIndexList);

        return getSearchData(sortedPagesByAbsRelevance, lemmasFromQuery);
    }

    private LinkedHashMap<Page, Float> getSortPagesWithAbsRelevance(List<Page> pages, List<Indexes> indexes) {
        Map<Page, Float> pageWithRelevance = pages.stream()
                .collect(Collectors.toMap(
                        page -> page,
                        page -> (float) indexes.stream()
                                .filter(index -> index.getPageEntity().equals(page))
                                .mapToDouble(Indexes::getLemmaRank)
                                .sum(),
                        (existing, replacement) -> existing
                ));


        float maxRelevance = pageWithRelevance.values().stream()
                .max(Float::compare)
                .orElse(0f);

        return pageWithRelevance.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() / maxRelevance,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<Page, Float>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }


    private List<SearchData> getSearchData(LinkedHashMap<Page, Float> sortedPages, List<String> lemmasFromQuery) {
        return sortedPages.entrySet().stream()
                .map(entry -> buildSearchData(entry.getKey(), entry.getValue(), lemmasFromQuery))
                .collect(Collectors.toList());
    }

    private SearchData buildSearchData(Page pageEntity, Float absRelevance, List<String> lemmasFromQuery) {
        String uri = pageEntity.getPath().substring(1);
        String content = pageEntity.getContent();
        String title = jsoupConnects.getTitleFromHtml(content);

        String siteName = pageEntity.getSiteEntity().getName();
        String site = getSiteUrl(siteName, uri);

        String clearContent = lemmaFinderUtil.removeHtmlTags(content);
        String snippet = getSnippet(clearContent, lemmasFromQuery);

        return new SearchData(site, siteName, uri, title, snippet, absRelevance);
    }

    private String getSiteUrl(String siteName, String uri) {
        if (!uri.contains("https:")) {
            switch (siteName) {
                case "playBack.ru":
                    return "https://www.playback.ru";
                case "skillbox.ru":
                    return "https://www.skillbox.ru";
                case "fparf.ru":
                    return "https://fparf.ru";
            }
        }
        return "";
    }

    private String getSnippet(String content, List<String> lemmasFromQuery) {
        List<Integer> lemmaIndex = lemmasFromQuery.stream()
                .flatMap(lemma -> lemmaFinderUtil.findLemmaIndexInText(content, lemma).stream())
                .sorted()
                .collect(Collectors.toList());

        List<String> wordsList = extractAndHighlightWordsByLemmaIndex(content, lemmaIndex);
        return wordsList.stream().limit(4).collect(Collectors.joining("... ")) + "...";
    }

    private List<String> extractAndHighlightWordsByLemmaIndex(String content, List<Integer> lemmaIndex) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < lemmaIndex.size(); i++) {
            int start = lemmaIndex.get(i);
            int end = content.indexOf(" ", start);
            end = (end == -1) ? content.length() : end;
            while (i + 1 < lemmaIndex.size() && lemmaIndex.get(i + 1) - end <= 5) {
                end = content.indexOf(" ", lemmaIndex.get(i + 1));
                i++;
            }

            String text = getWordsFromIndexWithHighlighting(start, end, content);
            result.add(text);
        }
        result.sort(Comparator.comparingInt(String::length).reversed());
        return result;
    }

    private String getWordsFromIndexWithHighlighting(int start, int end, String content) {
        String word = content.substring(start, end);
        int prevPoint = (content.lastIndexOf(" ", start) != -1) ? content.lastIndexOf(" ", start) : start;
        int lastPoint = (content.indexOf(" ", end + 30) != -1) ? content.indexOf(" ", end + 30) : content.length();

        String text = content.substring(prevPoint, lastPoint);
        try {
            text = text.replaceAll(Pattern.quote(word), "<b>" + word + "</b>");
        } catch (Exception e) {
            log.error("Ошибка выделения слов: {}", e.getMessage());
        }
        return text;
    }

    private List<SearchData> searchDataOffset(List<SearchData> searchData, int offset, int limit) {
        return searchData.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

}
