package rip.opencasket.stackexchange.user

data class UserDto(
	val id: Long,
	val username: String,
	val email: String,
	val firstName: String? = null,
	val lastName: String? = null
)

data class UserAuthoritiesDto(
	val id: Long,
	val username: String,
	val email: String,
	val authorities: Set<String>,
	val isEmailVerified: Boolean
)

data class UserCreationDto(
	val username: String,
	val email: String,
	val password: String
)