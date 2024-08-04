package rip.opencasket.stackexchange.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class BearerTokenAccessDeniedHandler : AccessDeniedHandler {

	override fun handle(
		request: HttpServletRequest,
		response: HttpServletResponse,
		accessDeniedException: AccessDeniedException
	) {
		response.setHeader("WWW-Authenticate", "Bearer")
		response.sendError(HttpServletResponse.SC_FORBIDDEN, accessDeniedException.message)
	}
}