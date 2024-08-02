package rip.opencasket.stackexchange.user

import jakarta.persistence.*
import org.hibernate.annotations.NaturalId
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "privileges")
@EntityListeners(AuditingEntityListener::class)
data class Privilege(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	var id: Long? = null,

	@NaturalId
	@Column(name = "name", unique = true, nullable = false)
	var name: String,

	@ManyToMany(mappedBy = "privileges")
	var roles: Set<Role> = mutableSetOf()
)