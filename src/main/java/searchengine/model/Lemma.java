package searchengine.model;

import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@AllArgsConstructor
@Table(name = "lemma", indexes = @Index(name = "lemma_index", columnList = "lemma, site_id, id", unique = true))
public class Lemma {

    @Id
    @SequenceGenerator(name = "lemma_seq_gen", sequenceName = "lemma_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lemma_seq_gen")
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @ForeignKey(name = "lemma_site_FK"),
            columnDefinition = "Integer",
            referencedColumnName = "id",
            name = "site_id",
            nullable = false,
            updatable = false)
    private Website siteEntity;

    @Column(name = "lemma", length = 255, nullable = false)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private int frequency;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "lemmaEntities")
    private Set<Page> pageEntities = new HashSet<>();




    public Lemma(Website siteEntity, String lemma, int frequency) {
        this.siteEntity = siteEntity;
        this.lemma = lemma;
        this.frequency = frequency;
    }

    public Lemma() {
    }


}