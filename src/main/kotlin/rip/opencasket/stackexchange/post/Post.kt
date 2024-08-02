package rip.opencasket.stackexchange.post

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
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
	open var id: Long? = null,

	@Version
	@Column(name = "lock_version")
	open var lockVersion: Long = 0,

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	open var createdAt: Instant? = null,

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	open var updatedAt: Instant? = null,

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false, updatable = false)
	open var author: User? = null,

	@Column(name = "content", nullable = false, columnDefinition = "text")
	open var content: String,

	@Column(name = "up_votes", nullable = false)
	open var upVotes: Int = 0,

	@Column(name = "down_votes", nullable = false)
	open var downVotes: Int = 0,

	@Column(name = "comment_count", nullable = false)
	open var commentCount: Int = 0,

	@OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
	open var comments: List<Comment> = mutableListOf(),

	@OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
	open var votes: List<PostVote> = mutableListOf(),

	@ManyToMany
	@JoinTable(
		name = "tags_on_posts",
		joinColumns = [JoinColumn(name = "post_id")],
		inverseJoinColumns = [JoinColumn(name = "tag_id")]
	)
	open var tags: List<Tag> = mutableListOf()
)

@Entity
@Table(name = "post_votes")
class PostVote(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	var id: Long? = null,

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	var createdAt: Instant? = null,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "post_id", nullable = false, updatable = false)
	var post: Post,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, updatable = false)
	var user: User,

	@Column(name = "vote_type", nullable = false)
	@Enumerated(EnumType.ORDINAL)
	var voteType: VoteType
)

@Entity
@DiscriminatorValue("1")
class Question(
	id: Long? = null,
	lockVersion: Long = 0,
	createdAt: Instant? = null,
	updatedAt: Instant? = null,
	author: User? = null,
	content: String,
	upVotes: Int = 0,
	downVotes: Int = 0,
	commentCount: Int = 0,
	comments: List<Comment> = mutableListOf(),
	votes: List<PostVote> = mutableListOf(),
	tags: List<Tag> = mutableListOf(),

	@Column(name = "title", nullable = false, columnDefinition = "text")
	var title: String,

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "accepted_answer_id")
	var acceptedAnswer: Answer? = null,

	@Column(name = "answer_count", nullable = false)
	var answerCount: Int = 0
) : Post(
	id = id,
	lockVersion = lockVersion,
	createdAt = createdAt,
	updatedAt = updatedAt,
	author = author,
	content = content,
	upVotes = upVotes,
	downVotes = downVotes,
	commentCount = commentCount,
	comments = comments,
	votes = votes,
	tags = tags
)

@Entity
@DiscriminatorValue("2")
class Answer(
	id: Long? = null,
	lockVersion: Long = 0,
	createdAt: Instant? = null,
	updatedAt: Instant? = null,
	author: User? = null,
	content: String,
	upVotes: Int = 0,
	downVotes: Int = 0,
	commentCount: Int = 0,
	comments: List<Comment> = mutableListOf(),
	votes: List<PostVote> = mutableListOf(),
	tags: List<Tag> = mutableListOf(),

	@ManyToOne
	@JoinColumn(name = "parent_id")
	var parent: Question,
) : Post(
	id = id,
	lockVersion = lockVersion,
	createdAt = createdAt,
	updatedAt = updatedAt,
	author = author,
	content = content,
	upVotes = upVotes,
	downVotes = downVotes,
	commentCount = commentCount,
	comments = comments,
	votes = votes,
	tags = tags
)

@Entity
@DiscriminatorValue("3")
class TagWiki(
	id: Long? = null,
	lockVersion: Long = 0,
	createdAt: Instant? = null,
	updatedAt: Instant? = null,
	author: User? = null,
	content: String,
	upVotes: Int = 0,
	downVotes: Int = 0,
	commentCount: Int = 0,
	comments: List<Comment> = mutableListOf(),
	votes: List<PostVote> = mutableListOf(),
	tags: List<Tag> = mutableListOf(),

	@OneToOne(optional = false, mappedBy = "post")
	var tag: Tag
) : Post(
	id = id,
	lockVersion = lockVersion,
	createdAt = createdAt,
	updatedAt = updatedAt,
	author = author,
	content = content,
	upVotes = upVotes,
	downVotes = downVotes,
	commentCount = commentCount,
	comments = comments,
	votes = votes,
	tags = tags
)