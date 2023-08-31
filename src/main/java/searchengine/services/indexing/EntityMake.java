package searchengine.services.indexing;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
@Component
public class EntityMake {

	private final SitesList sitesList;
	private final Environment environment;
	private final SiteRepository siteRepository;
	private final PageRepository pageRepository;
	private final LemmaRepository lemmaRepository;
	private final IndexRepository indexRepository;


	public ArrayList<SiteEntity> listSitesEntity() {
		sitesList.getSites().forEach(site -> System.out.println(site.getName()));
		indexRepository.deleteAllInBatch();
		lemmaRepository.deleteAllInBatch();
		pageRepository.deleteAllInBatch();
		siteRepository.deleteAllInBatch();
		ArrayList<SiteEntity> setSiteEntity = new ArrayList<>();
		sitesList.getSites().forEach(site -> setSiteEntity.add(newSiteEntity(site)));
		setSiteEntity.forEach(entity -> {siteRepository.save(entity);
		});
		return setSiteEntity;
	}

	private SiteEntity newSiteEntity(Site site) {
		SiteEntity siteEntity = new SiteEntity();
		siteEntity.setStatus(Status.INDEXING);
		siteEntity.setStatusTime(LocalDateTime.now());
		siteEntity.setLastError("");
		siteEntity.setUrl(site.getUrl());
		siteEntity.setName(site.getName());
		return siteEntity;
	}

	public SiteEntity oneSiteEntity(String site) {

		SiteEntity siteEntity = new SiteEntity();
		siteEntity.setStatus(Status.INDEXING);
		siteEntity.setStatusTime(LocalDateTime.now());
		siteEntity.setLastError("");
		siteEntity.setUrl(site);
		String name=site.replace("https://","");
		siteEntity.setName(name);
		siteRepository.save(siteEntity);
		return siteEntity;
	}




}
