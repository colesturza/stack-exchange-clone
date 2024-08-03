package rip.opencasket.stackexchange.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

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
