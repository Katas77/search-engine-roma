package searchengine.services.statistic;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final SitesList sites;


    @Override
    public StatisticsResponse getStatistics() {
        String[] statuses = { "INDEXED", "FAILED", "INDEXING" };
        String[] errors = {
                "Ошибка индексации: главная страница сайта не доступна",
                "Ошибка индексации: сайт не доступен",
                ""
        };
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(getTotal());
        data.setDetailed(getStatisticsData());
        response.setStatistics(data);
        response.setResult(true);
        return response;

    }
    private TotalStatistics getTotal() {
        long sites = siteRepository.count();
        if (siteRepository.count()==0)
        {sites= this.sites.getSites().size();}
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

}
