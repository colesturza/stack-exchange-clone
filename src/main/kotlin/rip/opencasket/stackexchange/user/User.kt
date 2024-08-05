package rip.opencasket.stackexchange.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Duration
import java.time.Instant

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener::class)
data class User(
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

	@Column(name = "username", unique = true, nullable = false)
	var username: String,

	@Column(name = "email", unique = true, nullable = false)
	var email: String,

	@Column(name = "password_hash", nullable = false)
	var passwordHash: String,

	@Column(name = "first_name")
	var firstName: String? = null,

	@Column(name = "last_name")
	var lastName: String? = null,

	@Column(name = "pronunciation")
	var pronunciation: String? = null,

	@Column(name = "pronouns")
	var pronouns: String? = null,

	@Column(name = "is_email_verified")
	var isEmailVerified: Boolean = false,

	@Column(name = "failed_login_attempts")
	var failedLoginAttempts: Int = 0,

	@Column(name = "locked_at")
	var lockedAt: Instant? = null,

	@Column(name = "locked_duration")
	var lockedDuration: Duration? = null,

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "users_roles",
		joinColumns = [JoinColumn(name = "user_id")],
		inverseJoinColumns = [JoinColumn(name = "role_id")]
	)
	var roles: Set<Role> = mutableSetOf()
)
