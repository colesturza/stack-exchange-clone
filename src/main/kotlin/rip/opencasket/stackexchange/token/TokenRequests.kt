package rip.opencasket.stackexchange.token

import jakarta.validation.constraints.NotBlank

data class TokenRequest(
	@field:NotBlank(message = "Username is mandatory")
	val username: String,

	@field:NotBlank(message = "Password is mandatory")
	val password: String
)

data class RefreshTokenRequest(
	@field:NotBlank(message = "Refresh token is mandatory")
	val refreshToken: String
)