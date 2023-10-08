package searchengine.model;

import lombok.*;

import javax.persistence.*;

@Setter
@Getter
@Entity
@Table(name = "search_index")
public class Indexes {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "FK_index_page_id"), name = "page_id", nullable = false)
    public Page pageEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "FK_index_lemma_id"), name = "lemma_id", nullable = false)
    public Lemma lemmaEntity;

    @Column(name = "lemma_rank", nullable = false)
    private float lemmaRank;

    public Indexes(Page pageEntity, Lemma lemmaEntity, float lemmaRank) {
        this.pageEntity = pageEntity;
        this.lemmaEntity = lemmaEntity;
        this.lemmaRank = lemmaRank;
    }

    public Indexes() {
    }
}