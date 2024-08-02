package rip.opencasket.stackexchange.token

import jakarta.persistence.*
import rip.opencasket.stackexchange.user.User
import java.time.Duration
import java.time.Instant

enum class TokenScope {
	ACTIVATION,
	AUTHENTICATION,
	PASSWORD_CHANGE
}

@Entity
@Table(name = "tokens")
data class Token(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	var id: Long? = null,

	@Column(name = "hash", unique = true, nullable = false, updatable = false)
	var hash: String,

	@Column(name = "expires_in", nullable = false, updatable = false)
	var expiresIn: Duration,

	@Column(name = "issued_at", nullable = false, updatable = false)
	var issuedAt: Instant,

	@Enumerated(EnumType.STRING)
	@Column(name = "scope", nullable = false, updatable = false)
	var scope: TokenScope,

	@ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = [CascadeType.ALL])
	@JoinColumn(name = "user_id", nullable = false, updatable = false)
	var user: User
)