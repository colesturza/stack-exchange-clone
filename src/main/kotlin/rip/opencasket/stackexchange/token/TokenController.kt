package rip.opencasket.stackexchange.token

import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.Instant

data class TokenRequest(
	@field:NotBlank(message = "Username is mandatory")
	val username: String,

	@field:NotBlank(message = "Password is mandatory")
	val password: String
)

data class TokenResponse(
	val token: String,
	val expiry: Instant,
)

@RestController
@RequestMapping("/tokens")
class TokenController(private val tokenService: TokenService) {

	@PostMapping("/authentication")
	fun authenticate(@RequestBody request: TokenRequest): ResponseEntity<TokenResponse> {
		val token = tokenService.authenticateUser(request.username, request.password)
		return if (token != null) {
			val response = TokenResponse(
				token = token.plaintext,
				expiry = token.expiry
			)
			ResponseEntity.ok(response)
		} else {
			ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
		}
	}

	@DeleteMapping("/authentication")
	fun removeAllAuthenticationTokens(authentication: Authentication): ResponseEntity<Void> {
		val currentUserId = authentication.principal as Long
		tokenService.removeAllAuthenticationTokens(currentUserId)
		return ResponseEntity.noContent().build()
	}
}
