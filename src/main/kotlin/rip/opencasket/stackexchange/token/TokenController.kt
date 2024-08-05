package rip.opencasket.stackexchange.token

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import rip.opencasket.stackexchange.security.CurrentUserId


@RestController
@RequestMapping("/tokens")
class TokenController(private val tokenService: TokenService) {

	@PostMapping("/authentication")
	fun authenticate(@Valid @RequestBody request: TokenRequest): ResponseEntity<Map<String, TokenResponse>> {
		val (authToken, refreshToken) = tokenService.authenticateUser(request.username, request.password)
		val response = mapOf(
			"authToken" to TokenResponse(authToken.plaintext, authToken.expiry),
			"refreshToken" to TokenResponse(refreshToken.plaintext, refreshToken.expiry)
		)
		return ResponseEntity.ok(response)
	}

	@DeleteMapping("/authentication")
	fun removeAllAuthenticationTokens(@CurrentUserId currentUserId: Long?): ResponseEntity<Void> {
		tokenService.unAuthenticateUser(currentUserId!!)
		return ResponseEntity.noContent().build()
	}

	@PostMapping("/refresh")
	fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<Map<String, TokenResponse>> {
		val (authToken, refreshToken) = tokenService.refreshAuthenticationToken(request.refreshToken)
		val response = mapOf(
			"authToken" to TokenResponse(authToken.plaintext, authToken.expiry),
			"refreshToken" to TokenResponse(refreshToken.plaintext, refreshToken.expiry)
		)
		return ResponseEntity.ok(response)
	}

	@PostMapping("/activation/new")
	fun createActivationToken(@Valid @RequestBody request: ActivationTokenRequest): ResponseEntity<Void> {
		tokenService.createNewActivationToken(request.email)
		return ResponseEntity.ok().build()
	}

	@PostMapping("/activation")
	fun activateUser(@Valid @RequestBody request: ActivateUserRequest): ResponseEntity<Void> {
		tokenService.activateUserAccount(request.activationToken)
		return ResponseEntity.ok().build()
	}

	@PostMapping("/password-reset/new")
	fun createPasswordRestToken(@Valid @RequestBody request: PasswordResetTokenRequest): ResponseEntity<Void> {
		tokenService.createNewPasswordResetToken(request.email)
		return ResponseEntity.ok().build()
	}

	@PostMapping("/password-reset")
	fun resetUserPassword(@Valid @RequestBody request: ResetUserPasswordRequest): ResponseEntity<Void> {
		tokenService.resetUserPassword(request.resetPasswordToken, request.newPassword)
		return ResponseEntity.ok().build()
	}
}
