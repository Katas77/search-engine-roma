package searchengine.services.indexing;
import searchengine.model.SiteEntity;

import java.util.ArrayList;
import java.util.Set;

public interface IndexingOperations {

	void startTreadsIndexing(ArrayList<SiteEntity> siteEntities);

	void startPartialIndexing(SiteEntity siteEntity);

	void setIndexingStarted(boolean value);


	void setIsActive(boolean value);
}

