package searchengine.utils.indexing;

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
import org.springframework.dao.DataIntegrityViolationException;
import searchengine.model.Page;
import searchengine.model.Website;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.Thread.sleep;
import static searchengine.utils.indexing.StringPool.*;

@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class RecursiveMake extends RecursiveAction {
    public static volatile Boolean isActive = true;
    public String siteUrl;
    private String currentUrl;
    private String parentPath;
    private Document document;
    private Page pageEntity;
    private Website siteEntity;
    private Set<String> childLinks;
    private Connection.Response response = null;
    private final PageRepository pageRepository;
    private BlockingQueue<Page> outcomeQueue;
    public static final ArrayList<String> html = new ArrayList<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    public static final String urlLink = "https?:/(?:/[^/]+)+/[А-Яа-яёЁ\\w\\W ]+\\.[\\wa-z]{2,5}(?!/|[\\wА-Яа-яёЁ])";
    public static final String urlValid = "^(ht|f)tp(s?)://[0-9a-zA-Z]([-.\\w]*[0-9a-zA-Z])*(:(0-9)*)*(/?)([a-zA-Z0-9\\-.,'=/\\\\+%_]*)?$";

    public RecursiveMake(String currentUrl,
                         Website siteEntity,
                         BlockingQueue<Page> outcomeQueue, PageRepository pageRepository, String siteUrl) {
        this.siteEntity = siteEntity;
        this.outcomeQueue = outcomeQueue;
        this.currentUrl = currentUrl;
        this.pageRepository = pageRepository;
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
            parentPath = "/" + currentUrl.replace(siteUrl, "");
            cleanHtmlContent();
            pageEntity = new Page(siteEntity, response.statusCode(), document.html(), parentPath);
        } catch (IOException | UncheckedIOException exception) {
            System.out.println(exception.getMessage());
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
                && !extractedHref.contains("#")
                && !extractedHref.equals(sourceUrl)
                && !newChildLinks.contains(extractedHref)
                &&
                (html.stream().anyMatch(extractedHref.substring(extractedHref.length() - 4)::contains)
                        | !extractedHref.matches(urlLink));
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
            try {
                pageRepository.save(pageEntity);
                internSavedPath(pageEntity.getPath());
                putPageEntityToQueue();
            } catch (DataIntegrityViolationException exception) {
                exception.getMessage();
            }
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
                try {
                    RecursiveMake action = new RecursiveMake(childLink, siteEntity, outcomeQueue, pageRepository, siteUrl);
                    action.fork();
                    subTasks.add(action);
                } catch (NullPointerException ignored) {
                } catch (DataIntegrityViolationException e) {
                    e.getMessage();
                }
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

    private void putPageEntityToQueue() {
        try {
            while (true) {
                if (outcomeQueue.remainingCapacity() < 5 && isActive)
                    sleep(5_000);
                else
                    break;
            }
            outcomeQueue.put(pageEntity);
        } catch (InterruptedException ex) {
            System.out.println(ex.getMessage());
        }
    }

}
