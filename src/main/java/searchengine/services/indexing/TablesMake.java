package searchengine.services.indexing;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.tools.UrlFormatter;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
@Component
public class TablesMake {

	private final SitesList sitesList;
	private final Environment environment;
	private final SiteRepository siteRepository;
	private final PageRepository pageRepository;
	private final LemmaRepository lemmaRepository;
	private final IndexRepository indexRepository;


	public Set<SiteEntity> setSites() {
		sitesList.getSites().forEach(site -> System.out.println(site.getName()));
		indexRepository.deleteAllInBatch();
		lemmaRepository.deleteAllInBatch();
		pageRepository.deleteAllInBatch();
		siteRepository.deleteAllInBatch();
		Set<SiteEntity> setSiteEntity = new HashSet<>();
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


	public SiteEntity partialInit(String url) {
		String path = "/" + url.replace(getHomeSiteUrl(url), "");
		String hostName = url.substring(0, url.lastIndexOf(path) + 1);
		Site site = findSiteInConfig(hostName);
		SiteEntity siteEntity;

		siteEntity = checkExistingSite(site);
		if (siteEntity == null)
			return null;

		if (!pageRepository.existsByPathAndSiteEntity(path, siteEntity)) {
			log.error("No pages with requested path contains in table page");
			log.info("Will try indexing this URL now");
		} else {
			List<PageEntity> pageEntities = getPageEntities(path, siteEntity);
			log.info("Found " + pageEntities.size() + " entity(ies) in table page by requested path " + path);

			decreaseLemmasFreqByPage(pageEntities);
		}
		siteEntity.setUrl(url);
		log.info(siteEntity.getUrl() + " will be indexing now");
		return siteEntity;
	}

	private String getHomeSiteUrl(String url) {
		String result = null;
		for (Site s : sitesList.getSites()) {
			if (s.getUrl().startsWith(UrlFormatter.getShortUrl(url))) {
				result = s.getUrl();
				break;
			}
		}
		return result;
	}

	@Nullable
	private SiteEntity checkExistingSite(Site site) {
		SiteEntity siteEntity;
		if (site == null) {
			log.error("SiteList doesn't contains hostname of requested URL");
			return null;
		} else {
			siteEntity = siteRepository.findByUrl(site.getUrl());
			if (siteEntity == null) {
				log.error("Table site doesn't contains entry with requested hostname");
				return null;
			}
		}
		return siteEntity;
	}

	private List<PageEntity> getPageEntities(String path, SiteEntity siteEntity) {
		List<PageEntity> pageEntities;
		if (Objects.equals(environment.getProperty("user-settings.delete-next-level-pages"), "true"))
			pageEntities = pageRepository.findAllBySiteEntityAndPathContains(siteEntity, path);
		else pageEntities = pageRepository.findBySiteEntityAndPath(siteEntity, path);
		return pageEntities;
	}

	private @Nullable Site findSiteInConfig(String hostName) {
		for (Site site : sitesList.getSites()) {
			if (site.getUrl()
					.toLowerCase(Locale.ROOT)
					.equals(hostName.toLowerCase(Locale.ROOT))) {
				return site;
			}
		}
		return null;
	}

	private void decreaseLemmasFreqByPage(List<PageEntity> pageEntities) {
		log.warn("Start decreasing freq of lemmas of deleted pages");

		List<IndexEntity> indexForLemmaDecreaseFreq = indexRepository.findAllByPageEntityIn(pageEntities);

		if (indexForLemmaDecreaseFreq == null) {
			log.error("Set of Index entities by Page is empty");
			return;
		}

		log.info("Found " + indexForLemmaDecreaseFreq.size() + " index entities by set of requested pages");
		pageRepository.deleteAllInBatch(pageEntities);

		for (IndexEntity indexObj : indexForLemmaDecreaseFreq) {
			LemmaEntity lemmaEntity = indexObj.getLemmaEntity();
			int oldFreq = lemmaEntity.getFrequency();

			if (oldFreq == 1) lemmaRepository.delete(lemmaEntity);
			else lemmaEntity.setFrequency(oldFreq - 1);
		}
	}


}
