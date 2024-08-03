package rip.opencasket.stackexchange.token

import java.time.Instant

data class TokenResponse(
	val token: String,
	val expiry: Instant
)