package rip.opencasket.stackexchange.user

import jakarta.persistence.*
import org.hibernate.annotations.NaturalId
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "roles")
@EntityListeners(AuditingEntityListener::class)
data class Role(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	var id: Long? = null,

	@NaturalId
	@Column(name = "name", unique = true, nullable = false)
	var name: String,

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
		name = "roles_privileges",
		joinColumns = [JoinColumn(name = "role_id")],
		inverseJoinColumns = [JoinColumn(name = "privilege_id")]
	)
	var privileges: Set<Privilege> = mutableSetOf(),

	@ManyToMany(mappedBy = "roles")
	var users: Set<User> = mutableSetOf()
)