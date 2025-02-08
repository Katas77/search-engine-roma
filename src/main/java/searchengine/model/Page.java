package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "page", indexes = @Index(name = "path_siteId_index", columnList = "path, site_id", unique = true))
public class Page {
    @Id
    @SequenceGenerator(name = "page_seq_gen", sequenceName = "page_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "page_seq_gen")
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Website.class, cascade = {CascadeType.MERGE, CascadeType.REFRESH}, optional = false)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(foreignKey = @ForeignKey(name = "site_page_FK"),
            columnDefinition = "Integer",
            referencedColumnName = "id",
            name = "site_id",
            nullable = false,
            updatable = false)
    private Website siteEntity;

    @NotNull
    @Column(name = "path", length = 255, nullable = false)
    private String path;

    @Column(nullable = false)
    private int code;

    @NotNull
    @Column(length = 4000, nullable = false)
    private String content;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "search_index",
            joinColumns = {@JoinColumn(name = "page_id")},
            inverseJoinColumns = {@JoinColumn(name = "lemma_id")})
    private Set<Lemma> lemmaEntities = new HashSet<>();



    public Page() {
    }

    public Page(Website siteEntity, int code, String content, String path) {
        this.siteEntity = siteEntity;
        this.path = path;
        this.code = code;
        this.content = content;
    }




}