package rip.opencasket.stackexchange.post.question

import jakarta.persistence.*
import rip.opencasket.stackexchange.post.Post
import rip.opencasket.stackexchange.post.PostVote
import rip.opencasket.stackexchange.post.tag.Tag
import rip.opencasket.stackexchange.post.answer.Answer
import rip.opencasket.stackexchange.post.comment.Comment
import rip.opencasket.stackexchange.user.User
import java.time.Instant

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
	comments: MutableList<Comment> = mutableListOf(),
	votes: MutableList<PostVote> = mutableListOf(),
	tags: MutableList<Tag> = mutableListOf(),

	@Column(name = "title", nullable = false, columnDefinition = "text")
	var title: String,

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "accepted_answer_id")
	var acceptedAnswer: Answer? = null,

	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
	var answers: MutableList<Answer> = mutableListOf(),

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