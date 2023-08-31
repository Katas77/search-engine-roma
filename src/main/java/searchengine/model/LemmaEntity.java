package searchengine.model;

import lombok.*;
import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@AllArgsConstructor
@Table(name = "lemma", indexes = @Index(name = "lemma_index", columnList = "lemma, site_id, id", unique = true))
public class LemmaEntity {

	@Id
	@Column(nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(foreignKey = @ForeignKey(name = "lemma_site_FK"), columnDefinition = "Integer",
			referencedColumnName = "id", name = "site_id", nullable = false, updatable = false)
	private SiteEntity siteEntity;

	@Column(columnDefinition = "VARCHAR(255)", nullable = false)
	private String lemma;

	@Column(nullable = false)
	private int frequency;

	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "lemmaEntities")
	private Set<PageEntity> pageEntities = new HashSet<>();


	public LemmaEntity(SiteEntity siteEntity, String lemma, int frequency) {
		this.siteEntity = siteEntity;
		this.lemma = lemma;
		this.frequency = frequency;
	}

	public LemmaEntity() {
	}


}