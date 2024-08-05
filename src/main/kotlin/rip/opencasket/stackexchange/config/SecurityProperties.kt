package rip.opencasket.stackexchange.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConfigurationProperties(prefix = "security")
data class SecurityProperties(
	/**
	 * The number of bytes to use when generating tokens.
	 */
	var bcryptStrength: Int = 12,

	/**
	 * The number of bytes to use when generating tokens.
	 */
	var tokenByteSize: Int = 32,

	/**
	 * The duration for which activation tokens are valid.
	 */
	var activationTokenExpiry: Duration = Duration.ofDays(3),

	/**
	 * The duration for which password reset tokens are valid.
	 */
	var passwordResetTokenExpiry: Duration = Duration.ofMinutes(15),

	/**
	 * The duration for which authentication tokens are valid.
	 */
	var authTokenExpiry: Duration = Duration.ofHours(1),

	/**
	 * The duration for which refresh tokens are valid.
	 */
	var refreshTokenExpiry: Duration = Duration.ofDays(30),

	/**
	 * The duration for which an account is locked after too many failed login attempts.
	 */
	var accountLockDuration: Duration = Duration.ofMinutes(15),

	/**
	 * The maximum number of failed login attempts before an account is locked.
	 */
	var maxFailedLoginAttempts: Int = 5
)