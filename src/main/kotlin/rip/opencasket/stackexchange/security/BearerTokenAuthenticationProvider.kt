package rip.opencasket.stackexchange.security

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import rip.opencasket.stackexchange.token.TokenExpiredException
import rip.opencasket.stackexchange.token.TokenNotFoundException
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

		val user = try {
			tokenService.findUserByScopeAndToken(TokenScope.AUTHENTICATION, bearerToken)
		} catch (ex: Exception) {
			when (ex) {
				is TokenNotFoundException -> throw BadCredentialsException("Token not found")
				is TokenExpiredException -> throw BadCredentialsException("Token expired")
				else -> throw ex
			}
		}

		val authorities = user.authorities.map { SimpleGrantedAuthority(it) }

		return BearerTokenAuthenticationToken(
			UserDetailsImpl(
				id = user.id,
				username = user.username,
				authorities = authorities,
				isEmailVerified = user.isEmailVerified,
			), bearerToken, authorities
		).apply {
			isAuthenticated = true
		}
	}

	override fun supports(authentication: Class<*>): Boolean {
		return BearerTokenAuthenticationToken::class.java.isAssignableFrom(authentication)
	}
}