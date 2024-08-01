package rip.opencasket.stackexchange.security

import org.slf4j.LoggerFactory
import org.springframework.data.domain.AuditorAware
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*

class AuditorAwareImpl : AuditorAware<Long> {

	companion object {
		private val logger = LoggerFactory.getLogger(AuditorAwareImpl::class.java)
	}

	override fun getCurrentAuditor(): Optional<Long> {
		val authentication = SecurityContextHolder.getContext().authentication

		if (authentication == null || !authentication.isAuthenticated) {
			logger.debug("No authentication found or user is not authenticated.")
			return Optional.empty()
		}

		return when (val principal = authentication.principal) {
			is Long -> {
				logger.debug("Current auditor ID: {}", principal)
				Optional.of(principal)
			}
			is String -> {
				if (principal == "anonymousUser") {
					logger.debug("Principal is 'anonymousUser'.")
					Optional.empty()
				} else {
					logger.warn("Unexpected String principal: $principal")
					Optional.empty()
				}
			}
			else -> {
				logger.warn("Unexpected principal type: ${principal::class.simpleName}")
				Optional.empty()
			}
		}
	}
}