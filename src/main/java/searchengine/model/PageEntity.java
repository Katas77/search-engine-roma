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
public class PageEntity {
	public PageEntity() {
	}
	public PageEntity(SiteEntity siteEntity, int code, String content, String path) {
		this.siteEntity = siteEntity;
		this.path = path;
		this.code = code;
		this.content = content;
	}

	@Id
	@Column(name = "id", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = SiteEntity.class, cascade = {CascadeType.MERGE, CascadeType.REFRESH}, optional = false)
	@OnDelete(action = OnDeleteAction.NO_ACTION)
	@JoinColumn(foreignKey = @ForeignKey(name = "site_page_FK"), columnDefinition = "Integer",
			referencedColumnName = "id", name = "site_id", nullable = false, updatable = false)
	private SiteEntity siteEntity;
	@NotNull
	@Column(name = "path", columnDefinition = "VARCHAR(255) CHARACTER SET utf8")
	private String path;

	@Column(nullable = false)
	private int code;
	@NotNull
	@Column(length = 4000, columnDefinition = "mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci", nullable = false)
	private String content;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "search_index",
			joinColumns = {@JoinColumn(name = "page_id")},
			inverseJoinColumns = {@JoinColumn(name = "lemma_id")})
	private Set<LemmaEntity> lemmaEntities = new HashSet<>();


}