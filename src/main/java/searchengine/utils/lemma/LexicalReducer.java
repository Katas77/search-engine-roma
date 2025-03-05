package searchengine.utils.lemma;

import searchengine.model.Page;
import searchengine.model.Website;

import java.util.concurrent.BlockingQueue;

public interface LexicalReducer {
    void setSiteEntity(Website siteEntity);

    void setQueue(BlockingQueue<Page> queueOfPagesForLemmasCollecting);

    void startCollecting() throws InterruptedException;
}



