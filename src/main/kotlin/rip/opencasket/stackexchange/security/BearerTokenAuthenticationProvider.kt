package rip.opencasket.stackexchange.security

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
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
			?: throw BadCredentialsException("Token or user not found")

		val authorities = user.authorities.map { SimpleGrantedAuthority(it) }

		return BearerTokenAuthenticationToken(user.id, bearerToken, authorities).apply {
			isAuthenticated = true
		}
	}

	override fun supports(authentication: Class<*>): Boolean {
		return BearerTokenAuthenticationToken::class.java.isAssignableFrom(authentication)
	}
}