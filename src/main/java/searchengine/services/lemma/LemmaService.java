package searchengine.services.lemma;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.concurrent.BlockingQueue;

public interface LemmaService {

	void setQueue(BlockingQueue<PageEntity> queueOfPagesForLemmasCollecting);
	void setSiteEntity(SiteEntity siteEntity);

	void startCollecting();
	void setOffOn(boolean value);
	LemmaEntity createLemmaEntity(String lemma);
	Boolean allowed();
	void setDone(boolean b);
}



