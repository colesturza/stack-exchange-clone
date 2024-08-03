package rip.opencasket.stackexchange.user

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserRepository : JpaRepository<User, Long> {
	fun findByUsername(username: String): Optional<User>
	fun findByEmail(email: String): Optional<User>
}