package rip.opencasket.stackexchange.security

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import rip.opencasket.stackexchange.token.TokenScope
import rip.opencasket.stackexchange.token.TokenService

class BearerTokenAuthenticationProvider(
	private val tokenService: TokenService
) : AuthenticationProvider {

	override fun authenticate(authentication: Authentication): Authentication {
		val bearerToken = authentication.credentials as? String
			?: throw BadCredentialsException("Bearer token must not be null or empty")

		if (bearerToken.isBlank() || bearerToken.length != 43) {
			throw BadCredentialsException("Invalid Bearer token")
		}

		val user = tokenService.findUserByScopeAndToken(TokenScope.AUTHENTICATION, bearerToken)
			?: throw BadCredentialsException("Token not found or user is null")

		return BearerTokenAuthenticationToken(user.id, bearerToken, null).apply {
			isAuthenticated = true
		}
	}

	override fun supports(authentication: Class<*>): Boolean {
		return BearerTokenAuthenticationToken::class.java.isAssignableFrom(authentication)
	}
}