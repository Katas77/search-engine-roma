package searchengine.dto.statistics;


import lombok.Data;


@Data
public class TotalStatistics {
    private int sites;
    private int pages;
    private int lemmas;
    private boolean indexing;

    public TotalStatistics(int sites, int pages, int lemmas, boolean indexing) {
        this.sites = sites;
        this.pages = pages;
        this.lemmas = lemmas;
        this.indexing = indexing;
    }
}
