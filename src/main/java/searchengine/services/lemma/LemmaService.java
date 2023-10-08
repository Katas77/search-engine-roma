package searchengine.services.lemma;

import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Website;

import java.util.concurrent.BlockingQueue;

public interface LemmaService {
    void setSiteEntity(Website siteEntity);

    void setQueue(BlockingQueue<Page> queueOfPagesForLemmasCollecting);

    void startCollecting();

    void setOffOn(boolean value);

    Boolean allowed();

    void setCycle(boolean b);
}



