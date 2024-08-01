package rip.opencasket.stackexchange.user

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.repository.Repository
import java.time.Instant

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener::class)
data class User(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	var id: Long? = null,

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

	@Version
	@Column(name = "lock_version")
	var lockVersion: Long = 0,

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	var createdAt: Instant? = null,

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant? = null,
)

interface UserRepository : Repository<User, Long> {
	fun save(user: User): User
	fun findById(id: Long): User?
	fun findByUsername(username: String): User?
	fun findByEmail(email: String): User?
}
