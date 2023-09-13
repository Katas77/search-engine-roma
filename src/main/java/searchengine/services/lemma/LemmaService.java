package searchengine.services.lemma;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Website;

import java.util.concurrent.BlockingQueue;

public interface LemmaService {

	void setQueue(BlockingQueue<Page> queueOfPagesForLemmasCollecting);
	void setSiteEntity(Website siteEntity);

	void startCollecting();
	void setOffOn(boolean value);
	Lemma createLemmaEntity(String lemma);
	Boolean allowed();
	void setDone(boolean b);
}



