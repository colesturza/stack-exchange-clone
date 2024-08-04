package rip.opencasket.stackexchange.post

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import rip.opencasket.stackexchange.user.User
import java.time.Instant

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