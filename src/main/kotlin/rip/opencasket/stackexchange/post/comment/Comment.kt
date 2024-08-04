package rip.opencasket.stackexchange.post.comment

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import rip.opencasket.stackexchange.post.Post
import rip.opencasket.stackexchange.user.User
import java.time.Instant


@Entity
@Table(name = "comments")
class Comment (
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	var id: Long? = null,

	@Version
	@Column(name = "lock_version", nullable = false)
	var lockVersion: Long = 0,

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	var createdAt: Instant? = null,

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant? = null,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false, updatable = false)
	var author: User,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "post_id", nullable = false, updatable = false)
	var post: Post,

	@Column(name = "content", nullable = false, columnDefinition = "text")
	var content: String,

	@Column(name = "up_votes", nullable = false)
	var upVotes: Int = 0,

	@Column(name = "down_votes", nullable = false)
	var downVotes: Int  = 0,

	@OneToMany(mappedBy = "comment", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
	var votes: MutableList<CommentVote> = mutableListOf()
)
