package rip.opencasket.stackexchange.security

import org.slf4j.LoggerFactory
import org.springframework.data.domain.AuditorAware
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
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
			is UserDetails -> {
				val userDetails = principal as? UserDetailsImpl
				userDetails?.let {
					logger.debug("Current auditor ID: {}", it.id)
					Optional.of(it.id)
				} ?: run {
					logger.warn("UserDetails principal is not of type UserDetailsImpl.")
					Optional.empty()
				}
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