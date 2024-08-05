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