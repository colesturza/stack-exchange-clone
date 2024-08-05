package rip.opencasket.stackexchange.token

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.NaturalId
import rip.opencasket.stackexchange.user.User
import java.time.Duration
import java.time.Instant

enum class TokenScope {
	ACTIVATION,
	AUTHENTICATION,
	REFRESH,
	PASSWORD_RESET
}

@Entity
@Table(name = "tokens")
data class Token(

	@Id
	@NaturalId
	@Column(name = "hash", unique = true, nullable = false, updatable = false)
	var hash: String,

	@Column(name = "expires_in", nullable = false, updatable = false)
	var expiresIn: Duration,

	@Column(name = "issued_at", nullable = false, updatable = false)
	var issuedAt: Instant,

	@Enumerated(EnumType.STRING)
	@Column(name = "scope", nullable = false, updatable = false)
	var scope: TokenScope,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, updatable = false)
	var user: User
)