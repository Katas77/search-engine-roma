package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import searchengine.dto.searh.SearchData;
import searchengine.dto.searh.SearchResponse;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.request.BadRequest;
import searchengine.services.indexing.JsoupConnect;


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
    private final LemmaFinderUtil lemmaFinderUtil;
    private final JsoupConnect jsoupConnects;

    @Override
    public ResponseEntity<Object> search(String query, String url, int offset, int limit) {
        System.out.println(query+"          "+url+"          "+offset+"          "+limit);
        if (query==null||query.isEmpty()) {
            return new ResponseEntity<>(new BadRequest(false, "Задан пустой поисковый запрос"),
                    HttpStatus.BAD_REQUEST);
        } else {
            List<SearchData> searchData;
            if (!url.isEmpty()) {
                if (siteRepository.findByUrl(url) == null) {
                    return new ResponseEntity<>(new BadRequest(false, "Данная страница находится за пределами сайтов, " +
                            "указанных в конфигурационном файле"),
                            HttpStatus.BAD_REQUEST);
                } else {
                    searchData = onePageSearch(query, url, offset, limit);
                }
            } else {
                searchData = searchThroughAllSites(query, offset, limit);
            }
            if (searchData == null) {
                return new ResponseEntity<>(new BadRequest(false, "По данному запросу ни чего не найдено."),
                        HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(new SearchResponse(true, searchData.size(), searchData), HttpStatus.OK);
        }
    }


    public List<SearchData> searchThroughAllSites(String query, int offset, int limit) {

        log.info( "-> Запускаем поиск по сайтам для запроса: "+ query);
        List<SiteEntity> sites = siteRepository.findAll();
        List<LemmaEntity> sortedLemmasPerSite = new ArrayList<>();

        List<String> lemmasFromQuery = getQueryIntoLemma(query);
        for (SiteEntity siteEntity : sites) {
            sortedLemmasPerSite.addAll(getLemmasFromSite(lemmasFromQuery, siteEntity));
        }
        List<SearchData> searchData = null;
        for (LemmaEntity lemmaEntity : sortedLemmasPerSite) {
            if (lemmaEntity.getLemma().equals(query)) {
                searchData = new ArrayList<>(getSearchDataList(sortedLemmasPerSite, lemmasFromQuery, offset, limit));
                searchData.sort((o1, o2) -> Float.compare(o2.getRelevance(), o1.getRelevance()));

            }
        }
        log.info("-> Поиск по сайтам завершен.");
        return searchData;
    }


    public List<SearchData> onePageSearch(String query, String url, int offset, int limit) {
        log.info( "-> Запускаем поиск по сайтам для запроса: "+ query);
        SiteEntity siteEntity = siteRepository.findByUrl(url);
        List<String> lemmasFromQuery = getQueryIntoLemma(query);
        List<LemmaEntity> lemmasFromSite = getLemmasFromSite(lemmasFromQuery, siteEntity);
        log.info("-> Поиск по сайтам завершен.");
        return getSearchDataList(lemmasFromSite, lemmasFromQuery, offset, limit);
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

    private List<LemmaEntity> getLemmasFromSite(List<String> lemmas, SiteEntity site) {
        ArrayList<LemmaEntity> lemmaList = (ArrayList<LemmaEntity>) lemmaRepository.findLemmasBySite(lemmas, site);
        lemmaList.sort(Comparator.comparingInt(LemmaEntity::getFrequency));
        return lemmaList;
    }

    private List<SearchData> getSearchDataList(List<LemmaEntity> lemmas, List<String> lemmasFromQuery,
                                               int offset, int limit) {
        List<SearchData> searchDataList = new ArrayList<>();
        if (lemmas.size() >= lemmasFromQuery.size()) {
            List<PageEntity> sortedPageList = pageRepository.findByLemmas(lemmas);
            List<IndexEntity> sortedIndexList = indexRepository.findByLemmasAndPages(lemmas, sortedPageList);
            LinkedHashMap<PageEntity, Float> sortedPagesByAbsRelevance =
                    getSortPagesWithAbsRelevance(sortedPageList, sortedIndexList);
           searchDataList = getSearchData(sortedPagesByAbsRelevance, lemmasFromQuery);

        }  return searchDataList;
    }

    private LinkedHashMap<PageEntity, Float> getSortPagesWithAbsRelevance(List<PageEntity> pages,
                                                                          List<IndexEntity> indexes) {
        HashMap<PageEntity, Float> pageWithRelevance = new HashMap<>();
        for (PageEntity page : pages) {
            float relevant = 0;
            for (IndexEntity index : indexes) {
                if (index.getPageEntity().equals(page)) {
                    relevant += index.getLemmaRank();
                }
            }
            pageWithRelevance.put(page, relevant);
        }
        HashMap<PageEntity, Float> pagesWithAbsRelevance = new HashMap<>();
        for (PageEntity page : pageWithRelevance.keySet()) {
            float absRelevant = pageWithRelevance.get(page) / Collections.max(pageWithRelevance.values());
            pagesWithAbsRelevance.put(page, absRelevant);
        }
        return pagesWithAbsRelevance
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private List<SearchData> getSearchData(LinkedHashMap<PageEntity, Float> sortedPages,
                                           List<String> lemmasFromQuey) {
        List<SearchData> searchData = new ArrayList<>();

        for (PageEntity pageEntity : sortedPages.keySet()) {
            String uri = pageEntity.getPath();
            String content = pageEntity.getContent();
            String title = jsoupConnects.getTitleFromHtml(content);
            SiteEntity siteEntity = pageEntity.getSiteEntity();
            String site = siteEntity.getUrl();
            String siteName = siteEntity.getName();
            Float absRelevance = sortedPages.get(pageEntity);
            String clearContent = lemmaFinderUtil.removeHtmlTags(content);
            String snippet = getSnippet(clearContent, lemmasFromQuey);
            searchData.add(new SearchData(site, siteName, uri, title, snippet, absRelevance));
        }
        return searchData;
    }


    private String getSnippet(String content, List<String> lemmasFromQuey) {
        List<Integer> lemmaIndex = new ArrayList<>();
        StringBuilder result = new StringBuilder();
        for (String lemma : lemmasFromQuey) {
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


}
