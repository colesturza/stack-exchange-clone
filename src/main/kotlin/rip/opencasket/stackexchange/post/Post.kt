package rip.opencasket.stackexchange.post

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import rip.opencasket.stackexchange.post.comment.Comment
import rip.opencasket.stackexchange.post.tag.Tag
import rip.opencasket.stackexchange.user.User
import java.time.Instant


@Entity
@Table(name = "posts")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "post_type", discriminatorType = DiscriminatorType.INTEGER)
abstract class Post(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	var id: Long? = null,

	@Version
	@Column(name = "lock_version")
	var lockVersion: Long = 0,

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	var createdAt: Instant? = null,

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant? = null,

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false, updatable = false)
	var author: User? = null,

	@Column(name = "content", nullable = false, columnDefinition = "text")
	var content: String,

	@Column(name = "up_votes", nullable = false)
	var upVotes: Int = 0,

	@Column(name = "down_votes", nullable = false)
	var downVotes: Int = 0,

	@Column(name = "comment_count", nullable = false)
	var commentCount: Int = 0,

	@OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
	var comments: MutableList<Comment> = mutableListOf(),

	@OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
	var votes: MutableList<PostVote> = mutableListOf(),

	@ManyToMany
	@JoinTable(
		name = "tags_on_posts",
		joinColumns = [JoinColumn(name = "post_id")],
		inverseJoinColumns = [JoinColumn(name = "tag_id")]
	)
	var tags: MutableList<Tag> = mutableListOf()
)
