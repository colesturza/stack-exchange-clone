package rip.opencasket.stackexchange.user

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

class UsernameAlreadyExistsException(message: String) : RuntimeException(message)
class EmailAlreadyExistsException(message: String) : RuntimeException(message)

data class UserDto(
	val id: Long,
	val username: String,
	val email: String,
	val firstName: String? = null,
	val lastName: String? = null
)

data class UserCreationDto(
	val username: String,
	val email: String,
	val password: String
)

@Service
class UserService(
	private val passwordEncoder: PasswordEncoder,
	private val userRepository: UserRepository
) {

	@Transactional
	fun create(@Valid @NotNull dto: UserCreationDto): UserDto {

		if (userRepository.findByUsername(dto.username) != null) {
			throw UsernameAlreadyExistsException("Username '${dto.username}' is already in use.")
		}

		if (userRepository.findByEmail(dto.email) != null) {
			throw EmailAlreadyExistsException("Email '${dto.email}' is already in use.")
		}

		val user = User(
			username = dto.username,
			email = dto.email,
			passwordHash = passwordEncoder.encode(dto.password)
		)
		val savedUser = userRepository.save(user)
		return UserDto(
			id = savedUser.id!!,
			username = savedUser.username,
			email = savedUser.email
		)
	}

	@Transactional(readOnly = true)
	fun findByUsername(username: String): UserDto? {
		val user = userRepository.findByUsername(username) ?: return null
		return UserDto(
			id = user.id!!,
			username = user.username,
			email = user.email,
			firstName = user.firstName,
			lastName = user.lastName
		)
	}

	@Transactional(readOnly = true)
	fun findById(id: Long): UserDto? {
		val user = userRepository.findById(id) ?: return null
		return UserDto(
			id = user.id!!,
			username = user.username,
			email = user.email,
			firstName = user.firstName,
			lastName = user.lastName
		)
	}

	@Transactional
	fun updateUser(id: Long, updateRequest: UserUpdateRequest): UserDto? {
		val user = userRepository.findById(id) ?: return null

		updateRequest.firstName?.let { user.firstName = it }
		updateRequest.lastName?.let { user.lastName = it }
		updateRequest.pronunciation?.let { user.pronunciation = it }
		updateRequest.pronouns?.let { user.pronouns = it }

		val updatedUser = userRepository.save(user)

		return UserDto(
			id = updatedUser.id!!,
			username = updatedUser.username,
			email = updatedUser.email,
			firstName = updatedUser.firstName,
			lastName = updatedUser.lastName
		)
	}
}