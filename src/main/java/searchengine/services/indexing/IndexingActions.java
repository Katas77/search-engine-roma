package searchengine.services.indexing;
import searchengine.model.SiteEntity;
import java.util.Set;

public interface IndexingActions {

	void startTreadsIndexing(Set<SiteEntity> siteEntities);

	void startPartialIndexing(SiteEntity siteEntity);

	void setIndexingActionsStarted(boolean value);

	boolean isIndexingActionsStarted();

	void setOffOn(boolean value);
}

