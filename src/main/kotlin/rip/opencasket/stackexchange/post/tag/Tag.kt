package rip.opencasket.stackexchange.post.tag

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import java.time.Instant


@Entity
@Table(name = "tags")
class Tag(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	var id: Long? = null,

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	var createdAt: Instant? = null,

	@Column(name = "name", nullable = false, unique = true, columnDefinition = "text")
	var name: String,

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "post_id", unique = true, nullable = false, updatable = false)
	var post: TagWiki,

	@OneToMany(mappedBy = "synonymOf", fetch = FetchType.LAZY)
	var synonyms: MutableList<Tag> = mutableListOf(),

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "synonym_of_id")
	var synonymOf: Tag? = null
) {

	fun addSynonym(synonym: Tag) {
		check(synonymOf == null) { "This tag already has a synonym" }
		synonyms.add(synonym)
		synonym.synonymOf = this
	}

	fun removeSynonym(synonym: Tag) {
		synonyms.remove(synonym)
		if (synonym.synonymOf === this) {
			synonym.synonymOf = null
		}
	}
}