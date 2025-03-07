package searchengine.utils.indexing;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.springframework.dao.DataIntegrityViolationException;
import searchengine.color.Colors;
import searchengine.model.Page;
import searchengine.model.Website;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class SubtaskDispatcher extends RecursiveAction {
    public static volatile Boolean isActive = true;
    private final String siteUrl;
    private final String currentUrl;
    private String parentPath;
    private Document document;
    private Page pageEntity;
    private final Website siteEntity;
    private Set<String> childLinks;
    private final PageRepository pageRepository;
    private final BlockingQueue<Page> outcomeQueue;
    private final Map<String, Boolean> links = new HashMap<>();
    private final Map<String, Boolean> pages404 = new HashMap<>();
    private final Map<String, Boolean> savedPaths = new HashMap<>();

    public SubtaskDispatcher(String currentUrl,
                             Website siteEntity,
                             BlockingQueue<Page> outcomeQueue,
                             PageRepository pageRepository,
                             String siteUrl) {
        this.siteEntity = siteEntity;
        this.outcomeQueue = outcomeQueue;
        this.currentUrl = currentUrl;
        this.pageRepository = pageRepository;
        this.siteUrl = siteUrl;
    }

    @Override
    protected void compute() {
        String data = "";
        if (!isActive) {
            return;
        }
        if (!links.containsKey(currentUrl)) {
            internVisitedLinks(currentUrl);
        }
        try {
            Thread.sleep(150);
            Connection.Response response = Jsoup.connect(currentUrl)
                    .ignoreHttpErrors(true)
                    .userAgent(new UserAgent().userAgentGet())
                    .referrer("http://www.google.com")
                    .execute();
            document = response.parse();
            parentPath = "/" + currentUrl.replace(siteUrl, "");
            cleanHtmlContent();
            if (document.html().length() > 4000) {
                data = document.html().substring(0, 3999);
            }
            pageEntity = new Page(siteEntity, response.statusCode(), data, parentPath);
        } catch (IOException | InterruptedException e) {
            log.error("Error parsing URL {}: {}", currentUrl, e.getMessage());
        }
        saveExtractedPage();
        final Elements elements = document.select("a[href]");
        if (!elements.isEmpty()) {
            childLinks = getChildLinks(elements);
        }
        forkAndJoinTasks();
    }

    private Set<String> getChildLinks(Elements elements) {
        Set<String> newChildLinks = new HashSet<>();
        for (Element element : elements) {
            final String href = wwwAdd(getHrefFromElement(element).toLowerCase());
            if (urlIsValidToProcess(newChildLinks, href)) {
                newChildLinks.add(href);
            }
        }
        return newChildLinks;
    }

    private boolean urlIsValidToProcess(Set<String> newChildLinks, String extractedHref) {
        return isImageAndDoc(extractedHref)
                && !newChildLinks.contains(extractedHref)
                && isImageAndDoc(extractedHref)
                && nameSiteContains(extractedHref);
    }

    private void cleanHtmlContent() {
        final String oldTitle = document.title();
        final Safelist safelist = Safelist.relaxed().preserveRelativeLinks(true);
        final Cleaner cleaner = new Cleaner(safelist);
        document = cleaner.clean(document);
        document.title(oldTitle);
    }

    private synchronized void saveExtractedPage() {
        if (!savedPaths.containsKey(parentPath)) {
            try {
                pageRepository.save(pageEntity);
                internSavedPath(pageEntity.getPath());
                putPageEntityToQueue();
            } catch (DataIntegrityViolationException exception) {
                log.error("Error saving page entity: {}", exception.getMessage());
            }
            log.info(Colors.ANSI_GREEN + "Save Page  with UR: {} from site {}" + Colors.ANSI_RESET, pageEntity.getPath(), pageEntity.getSiteEntity().getName());
        }
    }

    private void forkAndJoinTasks() {
        if (!isActive) {
            return;
        }

        List<SubtaskDispatcher> subTasks = new LinkedList<>();
        for (String childLink : childLinks) {
            if (childLink.startsWith("https:")
                    && !pages404.containsKey(childLink)
                    && !links.containsKey(childLink)) {
                try {
                    SubtaskDispatcher action = new SubtaskDispatcher(childLink, siteEntity, outcomeQueue, pageRepository, siteUrl);
                    action.fork();
                    subTasks.add(action);
                } catch (NullPointerException ignored) {
                } catch (DataIntegrityViolationException e) {
                    log.error("Error creating RecursiveMake task: {}", e.getMessage());
                }
            }
        }

        for (SubtaskDispatcher task : subTasks) {
            task.join();
        }
    }

    private String getHrefFromElement(Element element) {
        return (element != null) ? element.absUrl("href") : "";
    }

    private void putPageEntityToQueue() {
        try {
            while (true) {
                if (outcomeQueue.remainingCapacity() < 5 && isActive) {
                    Thread.sleep(50);
                } else {
                    break;
                }
            }
            outcomeQueue.put(pageEntity);
        } catch (InterruptedException ex) {
            log.error("Error putting page entity to queue: {}", ex.getMessage());
        }
    }

    private boolean isImageAndDoc(String link) {
        return !link.contains(".jpg")
                && !link.contains(".png")
                && !link.contains(".gif")
                && !link.contains(".webp")
                && !link.contains(".pdf")
                && !link.contains(".eps")
                && !link.contains(".xlsx")
                && !link.contains(".doc")
                && !link.contains(".pptx")
                && !link.contains(".docx")
                && !link.contains("?_ga");
    }

    private String wwwAdd(String url) {
        String newUrl = "";
        if (!url.startsWith("https://www.")) {
            newUrl = url.replace("https://", "https://www.");
        } else {
            newUrl = url;
        }
        return newUrl;
    }

    private boolean nameSiteContains(String href) {
        return href.toLowerCase().contains("skillbox") ||
                href.toLowerCase().contains("playback") ||
                href.toLowerCase().contains("fparf");
    }

    private void internVisitedLinks(String url) {
        links.put(url, true);
    }

    private void internSavedPath(String path) {
        savedPaths.put(path, true);
    }}