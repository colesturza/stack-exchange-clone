package rip.opencasket.stackexchange.user

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

data class UserCreationRequest(
	@field:NotBlank(message = "Username is mandatory")
	@field:Size(min = 6, message = "Username must have at least 6 characters")
	val username: String,

	@field:Email(message = "Email should be valid")
	@field:NotBlank(message = "Email is mandatory")
	val email: String,

	@field:NotBlank(message = "Password is mandatory")
	@field:Size(min = 8, message = "Password must be at least 8 characters")
	val password: String
)

data class UserUpdateRequest(
	@field:Size(max = 100, message = "First name must not exceed 100 characters")
	val firstName: String? = null,

	@field:Size(max = 100, message = "Last name must not exceed 100 characters")
	val lastName: String? = null,

	val pronunciation: String? = null,

	val pronouns: String? = null
)

data class UserResponse(
	val id: Long,
	val username: String,
	val email: String? = null,
	@JsonProperty(value = "first_name")
	val firstName: String? = null,
	@JsonProperty(value = "last_name")
	val lastName: String? = null
)

@RestController
@RequestMapping("users")
class UserController(private val userService: UserService) {

	@PostMapping
	fun createUser(@Valid @RequestBody request: UserCreationRequest): ResponseEntity<Void> {
		val user = userService.create(
			UserCreationDto(
				request.username,
				request.email,
				request.password
			)
		)

		val locationUri = ServletUriComponentsBuilder
			.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(user.id)
			.toUri()

		return ResponseEntity.created(locationUri).build()
	}

	@GetMapping("/me")
	fun getCurrentUser(authentication: Authentication): ResponseEntity<UserResponse> {
		val currentUserId = authentication.principal as Long

		val user = userService.findById(currentUserId) ?: return ResponseEntity.notFound().build()

		val response = UserResponse(
			id = user.id,
			username = user.username,
			email = user.email,
			firstName = user.firstName,
			lastName = user.lastName
		)
		return ResponseEntity.ok(response)
	}

	@PatchMapping("/me")
	fun updateUser(
		@Valid @RequestBody updateRequest: UserUpdateRequest,
		authentication: Authentication
	): ResponseEntity<UserResponse> {
		val currentUserId = authentication.principal as Long

		val updatedUser = userService.updateUser(currentUserId, updateRequest)
			?: return ResponseEntity.notFound().build()

		val response = UserResponse(
			id = updatedUser.id,
			username = updatedUser.username,
			email = updatedUser.email,
			firstName = updatedUser.firstName,
			lastName = updatedUser.lastName
		)

		return ResponseEntity.ok(response)
	}

	@GetMapping("/{username}")
	fun findByUsername(
		@PathVariable username: String,
		authentication: Authentication
	): ResponseEntity<UserResponse> {
		val currentUserId = authentication.principal as Long

		val user = userService.findByUsername(username) ?: return ResponseEntity.notFound().build()

		val response = if (user.id == currentUserId) {
			UserResponse(
				id = user.id,
				username = user.username,
				email = user.email,
				firstName = user.firstName,
				lastName = user.lastName
			)
		} else {
			UserResponse(
				id = user.id,
				username = user.username,
			)
		}

		return ResponseEntity.ok(response)
	}
}