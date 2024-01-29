package searchengine.utils.indexing;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
@Component
public class StringPool {
    public static Map<String, String> links;
    public static Map<String, String> savedPaths = null;
    public static Map<String, String> pages404;

    public StringPool() {
        savedPaths = new ConcurrentHashMap<>(3000);
        links = new ConcurrentHashMap<>(5000);
        pages404 = new ConcurrentHashMap<>(100);
    }

    public static void internVisitedLinks(String s) {
        String exist = links.putIfAbsent(s, s);
    }

    public static void internSavedPath(String s) {
        String exist = savedPaths.putIfAbsent(s, s);
    }

    public static void clearAll() {
        savedPaths.clear();
        links.clear();
        pages404.clear();
    }
}
