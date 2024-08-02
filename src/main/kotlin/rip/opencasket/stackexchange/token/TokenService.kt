package rip.opencasket.stackexchange.token

import com.google.common.hash.Hashing
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rip.opencasket.stackexchange.user.UserDto
import rip.opencasket.stackexchange.user.UserRepository
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.*

data class TokenDto(val plaintext: String, val expiry: Instant)

@Service
class TokenService(
	private val tokenRepository: TokenRepository,
	private val userRepository: UserRepository,
	private val passwordEncoder: PasswordEncoder
) {

	companion object {
		// Token byte size set to 32 bytes (256 bits) to ensure a high level of security.
		// This will produce a Base64-encoded string of 43 characters.
		private const val TOKEN_BYTE_SIZE = 32
		private val secureRandom = SecureRandom()
	}

	@Transactional
	fun authenticateUser(username: String, password: String): TokenDto? {
		val user = userRepository.findByUsername(username) ?: return null

		if (!passwordEncoder.matches(password, user.passwordHash)) {
			return null
		}

		val plaintext = generateToken()
		val expiresIn = Duration.ofHours(1)
		val issuedAt = Instant.now()

		val token = Token(
			hash = generateTokenHash(plaintext),
			expiresIn = expiresIn,
			issuedAt = issuedAt,
			scope = TokenScope.AUTHENTICATION,
			user = user
		)

		tokenRepository.save(token)

		return TokenDto(
			plaintext = plaintext,
			expiry = issuedAt.plus(expiresIn)
		)
	}

	@Transactional(readOnly = true)
	fun findUserByScopeAndToken(scope: TokenScope, token: String): UserDto? {
		val tokenHash = generateTokenHash(token)
		val tokenEntity = tokenRepository.findByScopeAndHash(scope, tokenHash) ?: return null
		return if (tokenEntity.issuedAt.plus(tokenEntity.expiresIn).isAfter(Instant.now())) {
			val user = tokenEntity.user
			UserDto(
				id = user.id!!,
				username = user.username,
				email = user.email,
				firstName = user.firstName,
				lastName = user.lastName,
			)
		} else {
			null
		}
	}

	@Transactional
	fun removeAllAuthenticationTokens(userId: Long) {
		tokenRepository.deleteByScopeAndUserId(TokenScope.AUTHENTICATION, userId)
	}

	private fun generateToken(): String {
		val randomBytes = ByteArray(TOKEN_BYTE_SIZE)
		secureRandom.nextBytes(randomBytes)
		return Base64.getEncoder().withoutPadding().encodeToString(randomBytes)
	}

	private fun generateTokenHash(plaintext: String): String {
		return Hashing.sha256().hashString(plaintext, StandardCharsets.UTF_8).toString()
	}
}
