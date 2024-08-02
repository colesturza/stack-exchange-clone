package rip.opencasket.stackexchange.user

import org.springframework.data.repository.Repository

interface UserRepository : Repository<User, Long> {
	fun save(user: User): User
	fun findById(id: Long): User?
	fun findByUsername(username: String): User?
	fun findByEmail(email: String): User?
}