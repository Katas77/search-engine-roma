package searchengine.services.statistic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import searchengine.config.SitesList;
import searchengine.dto.search.statistics.DetailedStatisticsItem;
import searchengine.dto.search.statistics.StatisticsData;
import searchengine.dto.search.statistics.StatisticsResponse;
import searchengine.dto.search.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;

import java.util.ArrayList;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor

public class StatisticsServiceImpl implements StatisticsService {

    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final SitesList sitesList;


    private TotalStatistics getTotal() {
        long sites = siteRepository.count();
        if (siteRepository.count()==0)
        {sites=sitesList.getSites().size();}
        long pages = pageRepository.count();
        long lemmas = lemmaRepository.count();
        return new TotalStatistics((int) sites, (int) pages, (int) lemmas, true);
    }

    private DetailedStatisticsItem getDetailed(SiteEntity site) {
        String url = site.getUrl();
        String name = site.getName();
        String status = site.getStatus().toString();
        LocalDateTime statusTime = site.getStatusTime();
        String error = site.getLastError();
        int pages = pageRepository.countBySiteEntity(site);
        int lemmas = lemmaRepository.countBySiteEntity(site);
        return new DetailedStatisticsItem(url, name, status, statusTime, error, pages, lemmas);
    }

    private List<DetailedStatisticsItem> getStatisticsData() {
        List<SiteEntity> sites = siteRepository.findAll();
        List<DetailedStatisticsItem> result = new ArrayList<>();
        for (SiteEntity site : sites) {
            DetailedStatisticsItem item = getDetailed(site);
            result.add(item);
        }
        return result;
    }


    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = getTotal();
        List<DetailedStatisticsItem> list = getStatisticsData();
        StatisticsData statistics = new StatisticsData(total, list);
        return new StatisticsResponse(true, statistics);
    }
}