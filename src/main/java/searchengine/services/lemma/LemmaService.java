package searchengine.services.lemma;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.concurrent.BlockingQueue;

public interface LemmaService {

	void setIncomeQueue(BlockingQueue<PageEntity> queueOfPagesForLemmasCollecting);
	void setSiteEntity(SiteEntity siteEntity);
//	void setSavingPagesIsDone(boolean b);
	void startCollecting();
	void setEnabled(boolean value);
	LemmaEntity createLemmaEntity(String lemma);
	Boolean allowed();
	void setScrapingIsDone(boolean b);
}



