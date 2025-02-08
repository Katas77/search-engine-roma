package searchengine.services.statistic;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Website;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final SitesList sites;

    @Override
    public StatisticsResponse getStatistics() {
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(getTotal());
        data.setDetailed(getStatisticsData());
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    private TotalStatistics getTotal() {
        long sitesCount = siteRepository.count();
        if (sitesCount == 0) {
            sitesCount = sites.getSites().size();
        }
        long pagesCount = pageRepository.count();
        long lemmasCount = lemmaRepository.count();
        return new TotalStatistics(Math.toIntExact(sitesCount), Math.toIntExact(pagesCount), Math.toIntExact(lemmasCount), true);
    }

    private DetailedStatisticsItem getDetailed(Website site) {
        String url = "https://www." + site.getName();
        String name = site.getName();
        String status = site.getStatus().name();
        LocalDateTime statusTime = site.getStatusTime();
        String error = site.getLastError();
        int pages = pageRepository.countBySiteEntity(site);
        int lemmas = lemmaRepository.countBySiteEntity(site);
        return new DetailedStatisticsItem(url, name, status, statusTime, error, pages, lemmas);
    }

    private List<DetailedStatisticsItem> getStatisticsData() {
        return siteRepository.findAll().stream()
                .map(this::getDetailed)
                .collect(Collectors.toList());
    }
}