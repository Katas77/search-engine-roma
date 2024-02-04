package searchengine.utils.lemma;

import searchengine.model.Page;
import searchengine.model.Website;

import java.util.concurrent.BlockingQueue;

public interface LemmaTools {
    void setSiteEntity(Website siteEntity);

    void setQueue(BlockingQueue<Page> queueOfPagesForLemmasCollecting);

    void startCollecting();

    void setOffOn(boolean value);

    Boolean allowed();

    void setCycle(boolean b);
}



