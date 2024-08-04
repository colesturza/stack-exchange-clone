package rip.opencasket.stackexchange.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository
import org.springframework.web.filter.OncePerRequestFilter


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
class BearerTokenAuthenticationFilter(private val authenticationManager: AuthenticationManager) :
	OncePerRequestFilter() {

	private var securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy()
	private var securityContextRepository = RequestAttributeSecurityContextRepository();

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
			filterChain.doFilter(request, response)
			return
		}

		val bearerToken = authenticationHeader.substringAfter("Bearer ", "").trim()
		BearerTokenAuthenticationFilter.logger.debug("Extracted Bearer token: $bearerToken")

		try {
			val authenticationToken = BearerTokenAuthenticationToken(null, bearerToken, null)
			val authentication = authenticationManager.authenticate(authenticationToken)
			val context = securityContextHolderStrategy.createEmptyContext()
			context.authentication = authentication
			securityContextHolderStrategy.context = context
			securityContextRepository.saveContext(context, request, response)
			BearerTokenAuthenticationFilter.logger.debug("Set SecurityContextHolder to {}", authentication)
		} catch (ex: AuthenticationException) {
			BearerTokenAuthenticationFilter.logger.debug("Authentication failed: {}", ex.message)
			throw ex
		}

		filterChain.doFilter(request, response)
	}
}