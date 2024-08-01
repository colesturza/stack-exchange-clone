package rip.opencasket.stackexchange.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.web.filter.OncePerRequestFilter
import rip.opencasket.stackexchange.token.TokenScope
import rip.opencasket.stackexchange.token.TokenService

/**
 * Filter to handle Bearer token authentication.
 *
 * This filter extracts a Bearer token from the "Authorization" header of the incoming HTTP request.
 * It validates the token against the TokenRepository, checking for its validity and expiration.
 * If the token is valid, it sets the authentication in the SecurityContext.
 * If not, it sends an unauthorized error response.
 *
 * @param tokenRepository the repository to retrieve and validate tokens
 */
class BearerTokenAuthenticationFilter(private val tokenService: TokenService) : OncePerRequestFilter() {

	companion object {
		private val logger = LoggerFactory.getLogger(BearerTokenAuthenticationFilter::class.java)
	}

	/**
	 * Processes the incoming request, extracts and validates the Bearer token from the Authorization header.
	 * Sets the authentication in the SecurityContext if the token is valid.
	 * Sends an unauthorized error response if the token is invalid or expired.
	 *
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @param filterChain the filter chain to pass the request and response to the next filter
	 */
	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain
	) {
		BearerTokenAuthenticationFilter.logger.debug("Processing request to ${request.requestURI}")

		val authenticationHeader = request.getHeader("Authorization")
		if (authenticationHeader.isNullOrEmpty() || !authenticationHeader.startsWith("Bearer ")) {
			BearerTokenAuthenticationFilter.logger.debug("No Bearer token found in Authorization header.")
			// No Authorization header or not a Bearer token, proceed with no authentication
			SecurityContextHolder.clearContext()
			filterChain.doFilter(request, response)
			return
		}

		val bearerToken = authenticationHeader.substringAfter("Bearer ", "").trim()
		BearerTokenAuthenticationFilter.logger.debug("Extracted Bearer token: $bearerToken")

		if (bearerToken.isBlank() || bearerToken.length != 43) {
			BearerTokenAuthenticationFilter.logger.debug("Bearer token is either blank or has an incorrect length.")
			sendUnauthorizedError(response)
			return
		}

		val userId = tokenService.findUserIdByScopeAndToken(TokenScope.AUTHENTICATION, bearerToken)
		if (userId == null) {
			BearerTokenAuthenticationFilter.logger.debug("Token not found in repository or associated user is null.")
			sendUnauthorizedError(response)
			return
		}

		BearerTokenAuthenticationFilter.logger.debug("Token is valid. Setting authentication in SecurityContext.")
		val authentication = PreAuthenticatedAuthenticationToken(userId, null, null)
		SecurityContextHolder.getContext().authentication = authentication

		filterChain.doFilter(request, response)
	}

	private fun sendUnauthorizedError(response: HttpServletResponse) {
		BearerTokenAuthenticationFilter.logger.debug("Sending unauthorized error response.")
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized.")
	}
}