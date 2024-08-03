package rip.opencasket.stackexchange.token

import java.time.Instant

data class TokenDto(val plaintext: String, val expiry: Instant)