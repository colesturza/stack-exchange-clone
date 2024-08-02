package rip.opencasket.stackexchange.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class BearerTokenAuthenticationToken(
	private val principal: Any?,
	private val credentials: Any?,
	authorities: Collection<GrantedAuthority>? = null
) : AbstractAuthenticationToken(authorities) {

	init {
		isAuthenticated = !authorities.isNullOrEmpty()
	}

	override fun getCredentials(): Any? {
		return credentials
	}

	override fun getPrincipal(): Any? {
		return principal
	}
}