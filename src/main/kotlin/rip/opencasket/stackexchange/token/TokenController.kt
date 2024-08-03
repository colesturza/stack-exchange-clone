package rip.opencasket.stackexchange.token

import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/tokens")
class TokenController(private val tokenService: TokenService) {

	@PostMapping("/authentication")
	fun authenticate(@RequestBody request: TokenRequest): ResponseEntity<Map<String, TokenResponse>> {
		val (authToken, refreshToken) = tokenService.authenticateUser(request.username, request.password)
		val response = mapOf(
			"authToken" to TokenResponse(authToken.plaintext, authToken.expiry),
			"refreshToken" to TokenResponse(refreshToken.plaintext, refreshToken.expiry)
		)
		return ResponseEntity.ok(response)
	}

	@PostMapping("/refresh")
	fun refresh(@RequestBody request: RefreshTokenRequest): ResponseEntity<Map<String, TokenResponse>> {
		val (authToken, refreshToken) = tokenService.refreshAuthenticationToken(request.refreshToken)
		val response = mapOf(
			"authToken" to TokenResponse(authToken.plaintext, authToken.expiry),
			"refreshToken" to TokenResponse(refreshToken.plaintext, refreshToken.expiry)
		)
		return ResponseEntity.ok(response)
	}

	@DeleteMapping("/authentication")
	fun removeAllAuthenticationTokens(authentication: Authentication): ResponseEntity<Void> {
		val currentUserId = authentication.principal as Long
		tokenService.unAuthenticateUser(currentUserId)
		return ResponseEntity.noContent().build()
	}
}
