package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Entity(name = "Website")
@Table(name = "site")

public class Website {

    public Website(Status status, LocalDateTime statusTime, String lastError, String url, String name) {
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
    }

    public Website() {
    }
    @Id
    @SequenceGenerator(name = "website_seq_gen", sequenceName = "website_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "website_seq_gen")
    @Column(name = "id", nullable = false)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "status_time", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "url", length = 255, nullable = false)
    private String url;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @OneToMany(mappedBy = "siteEntity", cascade = CascadeType.REMOVE)
    private Set<Page> pageEntities;

    @OneToMany(mappedBy = "siteEntity", cascade = CascadeType.REMOVE)
    private Set<Lemma> lemmaEntities;


}
