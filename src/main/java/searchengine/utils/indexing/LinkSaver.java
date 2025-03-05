package searchengine.utils.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.config.SitesList;
import searchengine.model.Status;
import searchengine.model.Website;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexing.IndexingServiceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
@Component
public class LinkSaver {
    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public List<Website> listSitesEntity() {
        clearRepositories();

        List<Website> siteEntities = createSiteEntities();
        siteEntities.forEach(siteRepository::save);

        if (!IndexingServiceImpl.oneUrl.isEmpty()) {
            siteRepository.deleteAllInBatch();
            saveSingleSiteEntity(IndexingServiceImpl.oneUrl);
        }

        List<Website> websiteList = siteRepository.findAll();
        websiteList.forEach(site -> log.info("Сохраненный сайт: {}", site.getName()));

        return websiteList;
    }

    private void clearRepositories() {
        indexRepository.deleteAllInBatch();
        lemmaRepository.deleteAllInBatch();
        pageRepository.deleteAllInBatch();
        siteRepository.deleteAllInBatch();
    }

    private List<Website> createSiteEntities() {
        List<Website> siteEntities = new ArrayList<>();
        sitesList.getSites().forEach(siteConfig -> siteEntities.add(createNewSiteEntity(siteConfig)));
        return siteEntities;
    }

    private Website createNewSiteEntity(searchengine.config.Site siteConfig) {
        Website siteEntity = new Website();
        siteEntity.setStatus(Status.INDEXING);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteEntity.setLastError("");
        siteEntity.setUrl(siteConfig.getUrl());
        siteEntity.setName(siteConfig.getName());
        return siteEntity;
    }

    private void saveSingleSiteEntity(String siteUrl) {
        Website siteEntity = new Website();
        siteEntity.setStatus(Status.INDEXING);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteEntity.setLastError("");
        siteEntity.setUrl(siteUrl);
        siteEntity.setName(extractSiteName(siteUrl));
        siteRepository.save(siteEntity);
    }

    private String extractSiteName(String siteUrl) {
        String[] nameArr = siteUrl.replace("https://", "").split("/");
        return nameArr[0];
    }
}
