package rip.opencasket.stackexchange.post.answer

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import rip.opencasket.stackexchange.post.Post
import rip.opencasket.stackexchange.post.PostVote
import rip.opencasket.stackexchange.post.comment.Comment
import rip.opencasket.stackexchange.post.question.Question
import rip.opencasket.stackexchange.post.tag.Tag
import rip.opencasket.stackexchange.user.User
import java.time.Instant

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
	comments: MutableList<Comment> = mutableListOf(),
	votes: MutableList<PostVote> = mutableListOf(),
	tags: MutableList<Tag> = mutableListOf(),

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "parent_id", nullable = false)
	var parent: Question

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