package rip.opencasket.stackexchange.user

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder


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
	fun getCurrentUser(@AuthenticationPrincipal currentUserId: Long): ResponseEntity<UserResponse> {
		val user = userService.findById(currentUserId)

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
		@AuthenticationPrincipal currentUserId: Long
	): ResponseEntity<UserResponse> {
		val updatedUser = userService.updateUser(currentUserId, updateRequest)

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
		@AuthenticationPrincipal currentUserId: Long
	): ResponseEntity<UserResponse> {

		val user = userService.findByUsername(username)

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