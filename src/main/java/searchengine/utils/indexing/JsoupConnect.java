package searchengine.utils.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class JsoupConnect {

    public Connection.Response Connect(String url) {
        Connection.Response response = null;
        try {
            response = Jsoup.connect(url).userAgent(new UserAgent().userAgentGet())
                    .referrer("http://www.google.com").execute();
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
        return response;
    }

    public String getTitleFromHtml(String content) {
        Document doc = Jsoup.parse(content);
        return doc.title();
    }

}
