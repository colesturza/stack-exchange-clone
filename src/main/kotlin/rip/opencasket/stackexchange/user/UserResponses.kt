package rip.opencasket.stackexchange.user

import com.fasterxml.jackson.annotation.JsonProperty

data class UserResponse(
	val id: Long,
	val username: String,
	val email: String? = null,
	@JsonProperty(value = "first_name")
	val firstName: String? = null,
	@JsonProperty(value = "last_name")
	val lastName: String? = null
)