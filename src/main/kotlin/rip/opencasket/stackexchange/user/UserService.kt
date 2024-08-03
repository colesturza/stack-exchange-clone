package rip.opencasket.stackexchange.user

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
	private val passwordEncoder: PasswordEncoder,
	private val userRepository: UserRepository
) {

	@Transactional
	fun create(@Valid @NotNull dto: UserCreationDto): UserDto {

		if (userRepository.findByUsername(dto.username).isPresent) {
			throw UsernameAlreadyExistsException("Username '${dto.username}' is already in use.")
		}

		if (userRepository.findByEmail(dto.email).isPresent) {
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
	fun findByUsername(username: String): UserDto {
		val user = userRepository.findByUsername(username).orElseThrow {
			UserNotFoundException("User with username '$username' not found.")
		}
		return UserDto(
			id = user.id!!,
			username = user.username,
			email = user.email,
			firstName = user.firstName,
			lastName = user.lastName
		)
	}

	@Transactional(readOnly = true)
	fun findById(id: Long): UserDto {
		val user = userRepository.findById(id).orElseThrow {
			UserNotFoundException("User with ID '$id' not found.")
		}
		return UserDto(
			id = user.id!!,
			username = user.username,
			email = user.email,
			firstName = user.firstName,
			lastName = user.lastName
		)
	}

	@Transactional
	fun updateUser(id: Long, updateRequest: UserUpdateRequest): UserDto {
		val user = userRepository.findById(id).orElseThrow {
			UserNotFoundException("User with ID '$id' not found.")
		}

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