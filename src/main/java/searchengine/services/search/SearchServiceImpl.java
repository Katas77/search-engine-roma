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
import searchengine.dto.forAll.GeneralRequest;
import searchengine.utils.indexing.JsoupConnect;
import searchengine.utils.indexing.RecursiveMake;
import searchengine.utils.searchandLemma.LemmaSearchTools;


import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
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
        try {
            Files.write(Paths.get("data/getChildLinksList.txt"), RecursiveMake.getChildLinksList);
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
        System.out.println(query + "    -   " + url + "    -      " + offset + "     -     " + limit);
        List<SearchData> searchData;
        if (!url.isEmpty()) {
            if (siteRepository.findByUrl(url) == null) {
                return new GeneralRequest().indexPageFailed();
            } else {
                searchData = onePageSearch(query, url);
            }
        } else {
            searchData = searchThroughAllSites(query);
        }
        if (query == null || query.isEmpty() || searchData == null) {
            searchData = new ArrayList<>();
            return new ResponseEntity<>(new SearchResponse(true, 0, searchData), HttpStatus.NOT_FOUND);
        }
        for (SearchData data : searchData) {
            System.out.println("'" + query + "'" + " - найден:");
            if (data.getSiteName().equals("playBack.ru") & !data.getUri().contains("http")) {
                System.out.println("https://" + data.getSiteName() + data.getUri());
            } else
                System.out.println(data.getUri());
        }
        return new ResponseEntity<>(new SearchResponse(true, searchData.size(), searchDataOffset(searchData, offset, limit)), HttpStatus.OK);
    }


    public List<SearchData> searchThroughAllSites(String query) {
        log.info("Запускаем поиск по сайтам для запроса: " + query);
        List<Website> sites = siteRepository.findAll();
        List<Lemma> sortedLemmasPerSite = new ArrayList<>();
        List<String> lemmasFromQuery = getQueryIntoLemma(query);
        for (Website siteEntity : sites) {
            sortedLemmasPerSite.addAll(getLemmasFromSite(lemmasFromQuery, siteEntity));
        }
        List<SearchData> searchData = null;
        searchData = new ArrayList<>(getSearchDataList(sortedLemmasPerSite, lemmasFromQuery));
        searchData.sort((o1, o2) -> Float.compare(o2.getRelevance(), o1.getRelevance()));
        log.info(" Поиск по сайтам завершен.");
        return searchData;
    }


    public List<SearchData> onePageSearch(String query, String url) {
        log.info("Запускаем поиск по сайтам для запроса: " + query);
        Website siteEntity = siteRepository.findByUrl(url);
        List<String> lemmasFromQuery = getQueryIntoLemma(query);
        List<Lemma> lemmasFromSite = getLemmasFromSite(lemmasFromQuery, siteEntity);
        log.info("Поиск по сайтам завершен.");
        return getSearchDataList(lemmasFromSite, lemmasFromQuery);
    }

    private List<String> getQueryIntoLemma(String query) {
        String[] words = query.toLowerCase(Locale.ROOT).split(" ");
        List<String> lemmaList = new ArrayList<>();
        for (String word : words) {
            List<String> lemma = lemmaFinderUtil.getLemma(word);
            lemmaList.addAll(lemma);
        }
        return lemmaList;
    }

    private List<Lemma> getLemmasFromSite(List<String> lemmas, Website site) {
        ArrayList<Lemma> lemmaList = (ArrayList<Lemma>) lemmaRepository.findLemmasBySite(lemmas, site);
        lemmaList.sort(Comparator.comparingInt(Lemma::getFrequency));
        return lemmaList;
    }

    private List<SearchData> getSearchDataList(List<Lemma> lemmas, List<String> lemmasFromQuery) {
        List<SearchData> searchDataList = new ArrayList<>();
        if (lemmas.size() >= lemmasFromQuery.size()) {
            List<Page> sortedPageList = pageRepository.findByLemmas(lemmas);
            List<Indexes> sortedIndexList = indexRepository.findByLemmasAndPages(lemmas, sortedPageList);
            LinkedHashMap<Page, Float> sortedPagesByAbsRelevance =
                    getSortPagesWithAbsRelevance(sortedPageList, sortedIndexList);
            searchDataList = getSearchData(sortedPagesByAbsRelevance, lemmasFromQuery);
        }
        return searchDataList;
    }

    private LinkedHashMap<Page, Float> getSortPagesWithAbsRelevance(List<Page> pages, List<Indexes> indexes) {
        HashMap<Page, Float> pageWithRelevance = new HashMap<>();
        for (Page page : pages) {
            float relevant = 0;
            for (Indexes index : indexes) {
                if (index.getPageEntity().equals(page)) {
                    relevant += index.getLemmaRank();
                }
            }
            pageWithRelevance.put(page, relevant);
        }
        HashMap<Page, Float> pagesWithAbsRelevance = new HashMap<>();
        for (Page page : pageWithRelevance.keySet()) {
            float absRelevant = pageWithRelevance.get(page) / Collections.max(pageWithRelevance.values());
            pagesWithAbsRelevance.put(page, absRelevant);
        }
        return pagesWithAbsRelevance
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private List<SearchData> getSearchData(LinkedHashMap<Page, Float> sortedPages,
                                           List<String> lemmasFromQuery) {
        List<SearchData> searchData = new ArrayList<>();
        for (Page pageEntity : sortedPages.keySet()) {
            String uri = pageEntity.getPath().substring(1);
            String content = pageEntity.getContent();
            String title = jsoupConnects.getTitleFromHtml(content);
            Website siteEntity = pageEntity.getSiteEntity();
            String siteName = siteEntity.getName();
            String site = "";
            if (siteName.equals("playBack.ru") & !uri.contains("http")) {
                site = "https://" + siteName;
                uri = pageEntity.getPath();
            }
            Float absRelevance = sortedPages.get(pageEntity);
            String clearContent = lemmaFinderUtil.removeHtmlTags(content);
            String snippet = getSnippet(clearContent, lemmasFromQuery);
            searchData.add(new SearchData(site, siteName, uri, title, snippet, absRelevance));
        }
        return searchData;
    }


    private String getSnippet(String content, List<String> lemmasFromQuery) {
        List<Integer> lemmaIndex = new ArrayList<>();
        StringBuilder result = new StringBuilder();
        for (String lemma : lemmasFromQuery) {
            lemmaIndex.addAll(lemmaFinderUtil.findLemmaIndexInText(content, lemma));
        }
        Collections.sort(lemmaIndex);
        List<String> wordsList = extractAndHighlightWordsByLemmaIndex(content, lemmaIndex);
        for (int i = 0; i < wordsList.size(); i++) {
            result.append(wordsList.get(i)).append("... ");
            if (i > 3) {
                break;
            }
        }
        return result.toString();
    }

    private List<String> extractAndHighlightWordsByLemmaIndex(String content, List<Integer> lemmaIndex) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < lemmaIndex.size(); i++) {
            int start = lemmaIndex.get(i);
            int end = content.indexOf(" ", start);
            int step = i + 1;
            while (step < lemmaIndex.size() && lemmaIndex.get(step) - end > 0 && lemmaIndex.get(step) - end < 5) {
                end = content.indexOf(" ", lemmaIndex.get(step));
                step += 1;
            }
            i = step - 1;
            String text = getWordsFromIndexWithHighlighting(start, end, content);
            result.add(text);
        }
        result.sort(Comparator.comparingInt(String::length).reversed());
        return result;
    }

    private String getWordsFromIndexWithHighlighting(int start, int end, String content) {
        String word = content.substring(start, end);
        int prevPoint;
        int lastPoint;
        if (content.lastIndexOf(" ", start) != -1) {
            prevPoint = content.lastIndexOf(" ", start);
        } else prevPoint = start;
        if (content.indexOf(" ", end + 30) != -1) {
            lastPoint = content.indexOf(" ", end + 30);
        } else lastPoint = content.indexOf(" ", end);
        String text = content.substring(prevPoint, lastPoint);
        try {
            text = text.replaceAll(word, "<b>" + word + "</b>");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return text;
    }

    private List<SearchData> searchDataOffset(List<SearchData> searchData, int offset, int limit) {
        int a = limit + offset;
        if (a > searchData.size()) {
            a = searchData.size();
        }
        return searchData.subList(offset, a);
    }
}
