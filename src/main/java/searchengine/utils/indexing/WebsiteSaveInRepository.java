package searchengine.utils.indexing;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexing.IndexingServiceImpl;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
@Component
public class WebsiteSaveInRepository {
    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;


    public List<Website> listSitesEntity() {
        indexRepository.deleteAllInBatch();
        lemmaRepository.deleteAllInBatch();
        pageRepository.deleteAllInBatch();
        siteRepository.deleteAllInBatch();
        ArrayList<Website> list = new ArrayList<>();
        sitesList.getSites().forEach(site -> list.add(newSiteEntity(site)));
        list.forEach(siteRepository::save);
        if (IndexingServiceImpl.oneUrl != "") {
            siteRepository.deleteAllInBatch();
            oneSiteEntity(IndexingServiceImpl.oneUrl);
        }
        List<Website> websiteList = siteRepository.findAll();
        websiteList.forEach(site -> System.out.println(site.getName()));
        return websiteList;
    }

    private Website newSiteEntity(searchengine.config.Site site) {
        Website siteEntity = new Website();
        siteEntity.setStatus(Status.INDEXING);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteEntity.setLastError("");
        siteEntity.setUrl(site.getUrl());
        siteEntity.setName(site.getName());
        return siteEntity;
    }

    public Website oneSiteEntity(String site) {
        Website siteEntity = new Website();
        siteEntity.setStatus(Status.INDEXING);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteEntity.setLastError("");
        siteEntity.setUrl(site);
        String[] nameArr = site.replace("https://", "").split("/");
        siteEntity.setName(nameArr[0]);
        siteRepository.save(siteEntity);
        return siteEntity;
    }


}
