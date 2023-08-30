package searchengine.services.indexing;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UncheckedIOException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.springframework.core.env.Environment;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import static java.lang.Thread.sleep;
import static searchengine.services.indexing.StringPool.*;

@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class RecursiveMake extends RecursiveAction {

	public static volatile Boolean isActive = true;
	public String homeUrl;
	public String siteUrl;
	private String currentUrl;
	private Document document;
	private String parentPath;
	private SiteEntity siteEntity;
	private PageEntity pageEntity;
	private Set<String> childLinks;
	private Connection.Response response = null;
	private BlockingQueue<PageEntity> outcomeQueue;
	private Environment environment;
	private final PageRepository pageRepository;
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	public static final String urlLink = "https?:/(?:/[^/]+)+/[А-Яа-яёЁ\\w\\W ]+\\.[\\wa-z]{2,5}(?!/|[\\wА-Яа-яёЁ])";
	public static final String urlValid = "^(ht|f)tp(s?)://[0-9a-zA-Z]([-.\\w]*[0-9a-zA-Z])*(:(0-9)*)*(/?)([a-zA-Z0-9\\-.,'=/\\\\+%_]*)?$";
	public static final ArrayList<String> html = new ArrayList<>();

	public RecursiveMake(String currentUrl,
						 SiteEntity siteEntity,
						 BlockingQueue<PageEntity> outcomeQueue, Environment environment, PageRepository pageRepository, String homeUrl, String siteUrl) {
		this.siteEntity = siteEntity;
		this.outcomeQueue = outcomeQueue;
		this.currentUrl = currentUrl;
		this.environment = environment;
		this.pageRepository = pageRepository;
		this.homeUrl = homeUrl;
		this.siteUrl = siteUrl;
	}
	@Override
	protected void compute() {
		if (!isActive)
			return;
		lock.readLock().lock();

		if (!links.containsKey(currentUrl))
			internVisitedLinks(currentUrl);
		lock.readLock().unlock();
		try {
			response = Jsoup.connect(currentUrl).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
					.referrer("http://www.google.com").execute();

			document = response.parse();
			parentPath = "/" + currentUrl.replace(homeUrl, "");
			cleanHtmlContent();
			pageEntity = new PageEntity(siteEntity, response.statusCode(), document.html(), parentPath);
		} catch (IOException | UncheckedIOException exception) {
			urlNotAvailableActions(currentUrl, exception);
		}
		if (Objects.equals(environment.getProperty("user-settings.logging-enable"), "true")) {
			log.info("Response from " + currentUrl + " got successfully");
		}
		saveExtractedPage();
		final Elements elements = document.select("a[href]");
		if (!elements.isEmpty()) {
			childLinks = getChildLinks(currentUrl, elements);
		}
		forkAndJoinTasks();
	}
	public Set<String> getChildLinks(String url, Elements elements) {
		Set<String> newChildLinks = new HashSet<>();
		for (Element element : elements) {
			final String href = getHrefFromElement(element).toLowerCase();
			lock.readLock().lock();
			if (links.containsKey(href))
				continue;
			else if (urlIsValidToProcess(url, newChildLinks, href)) {
				addHrefToOutcomeValue(newChildLinks, href);
			}
			lock.readLock().unlock();
		}
		return newChildLinks;
	}

	private void addHrefToOutcomeValue(Set<String> newChildLinks, String href) {
		if (!links.containsKey(href)
				&& !pages404.containsKey(href)
				&& !savedPaths.containsKey(href)) {
			newChildLinks.add(href);
		}
	}

	private boolean urlIsValidToProcess(String sourceUrl, Set<String> newChildLinks, String extractedHref) {
		return sourceUrl.matches(urlValid)
				&& extractedHref.startsWith(siteUrl)
				&& !extractedHref.contains("#")
				&& !extractedHref.equals(sourceUrl)
				&& !newChildLinks.contains(extractedHref)
				&&
				(html.stream().anyMatch(extractedHref.substring(extractedHref.length() - 4)::contains)
						| !extractedHref.matches(urlLink));
	}


	private void urlNotAvailableActions(String url, Exception exception) {
		siteEntity.setLastError(exception.getMessage());
		siteEntity.setStatusTime(LocalDateTime.now());
		internPage404(url);
		if (Objects.equals(environment.getProperty("user-settings.logging-enable"), "true")) {
			log.error("Something went wrong 404. " + url + " Pages404 vault contains " + pages404.size() + " url");
		}
	}

	private void cleanHtmlContent() {
		final String oldTitle = document.title();
		final Safelist safelist = Safelist.relaxed().preserveRelativeLinks(true);
		final Cleaner cleaner = new Cleaner(safelist);
		boolean isValid = cleaner.isValid(document);
		if (!isValid) {
			document = cleaner.clean(document);
			document.title(oldTitle);
		}
	}
	private void saveExtractedPage() {
		lock.readLock().lock();
		if (!savedPaths.containsKey(parentPath)) {
			pageRepository.save(pageEntity);
			internSavedPath(pageEntity.getPath());
			putPageEntityToOutcomeQueue();
			writeLogAboutEachPage();
		}
		lock.readLock().unlock();
	}

	private void forkAndJoinTasks() {
		if (!isActive)
			return;

		List<RecursiveMake> subTasks = new LinkedList<>();

		for (String childLink : childLinks) {
			if (childIsValidToFork(childLink)
					&& !pages404.containsKey(childLink)
					&& !links.containsKey(childLink)) {
				RecursiveMake action = new RecursiveMake(childLink, siteEntity, outcomeQueue, environment, pageRepository, homeUrl, siteUrl);
				action.fork();
				subTasks.add(action);
			}
		}
		for (RecursiveMake task : subTasks) task.join();
	}

	private boolean childIsValidToFork(String subLink) {
		final String ext = subLink.substring(subLink.length() - 4);
		html.add("html");
		html.add("dhtml");
		html.add("shtml");
		html.add("xhtml");
		return (html.stream().anyMatch(ext::contains) | !subLink.matches(urlLink));
	}

	public String getHrefFromElement(Element element) {
		return (element != null) ? element.absUrl("href") : "";
	}

	private void writeLogAboutEachPage() {
		if (Objects.equals(environment.getProperty("user-settings.logging-enable"), "true")) {
			log.warn(pageEntity.getPath() + " saved");
		}
	}

	private void putPageEntityToOutcomeQueue() {
		try {
			while (true) {
				if (outcomeQueue.remainingCapacity() < 5 && isActive)
					sleep(5_000);
				else
					break;
			}
			outcomeQueue.put(pageEntity);
		} catch (InterruptedException ex) {
			log.error("Can't put pageEntity to outcomeQueue");
		}
	}

}
