package rip.opencasket.stackexchange.token

import com.google.common.hash.Hashing
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rip.opencasket.stackexchange.user.User
import rip.opencasket.stackexchange.user.UserAuthoritiesDto
import rip.opencasket.stackexchange.user.UserRepository
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.*

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
	fun authenticateUser(username: String, password: String): Pair<TokenDto, TokenDto> {
		val user = userRepository.findByUsername(username).orElseThrow {
			UsernameNotFoundException("User with username '$username' not found.")
		}

		if (!passwordEncoder.matches(password, user.passwordHash)) {
			throw InvalidCredentialsException("Invalid username or password.")
		}

		return generateAndSaveNewAuthenticationTokens(user)
	}

	@Transactional
	fun refreshAuthenticationToken(refreshToken: String): Pair<TokenDto, TokenDto> {
		val tokenEntity = tokenRepository.findByScopeAndHash(TokenScope.REFRESH, generateTokenHash(refreshToken))
			?: throw TokenNotFoundException("Refresh token not found or expired.")

		if (tokenEntity.issuedAt.plus(tokenEntity.expiresIn).isBefore(Instant.now())) {
			throw TokenExpiredException("Refresh token has expired.")
		}

		val user = tokenEntity.user

		return generateAndSaveNewAuthenticationTokens(user)
	}

	@Transactional
	fun unAuthenticateUser(userId: Long) {
		removeAllTokensByScopeAndUser(TokenScope.AUTHENTICATION, userId)
		removeAllTokensByScopeAndUser(TokenScope.REFRESH, userId)
	}

	@Transactional(readOnly = true)
	fun findUserByScopeAndToken(scope: TokenScope, token: String): UserAuthoritiesDto {
		val tokenEntity = validateToken(token, scope)
		val user = tokenEntity.user
		val roleNames = user.roles.map { it.name }
		val privilegeNames = user.roles.flatMap { role -> role.privileges.map { privilege -> privilege.name } }
		val authorities = (roleNames + privilegeNames).toSet()

		return UserAuthoritiesDto(
			id = user.id!!,
			username = user.username,
			email = user.email,
			authorities = authorities
		)
	}

	private fun removeAllTokensByScopeAndUser(scope: TokenScope, userId: Long) {
		tokenRepository.deleteByScopeAndUserId(scope, userId)
	}

	private fun createToken(user: User, scope: TokenScope, expiresIn: Duration): TokenDto {
		val plaintext = generateToken()
		val issuedAt = Instant.now()

		val token = Token(
			hash = generateTokenHash(plaintext),
			expiresIn = expiresIn,
			issuedAt = issuedAt,
			scope = scope,
			user = user
		)

		tokenRepository.save(token)

		return TokenDto(
			plaintext = plaintext,
			expiry = issuedAt.plus(expiresIn)
		)
	}

	private fun generateAndSaveNewAuthenticationTokens(user: User): Pair<TokenDto, TokenDto> {
		removeAllTokensByScopeAndUser(TokenScope.AUTHENTICATION, user.id!!)
		removeAllTokensByScopeAndUser(TokenScope.REFRESH, user.id!!)

		val authToken = createToken(user, TokenScope.AUTHENTICATION, Duration.ofHours(1))
		val refreshToken = createToken(user, TokenScope.REFRESH, Duration.ofDays(30))

		return Pair(authToken, refreshToken)
	}

	private fun validateToken(token: String, scope: TokenScope): Token {
		val tokenHash = generateTokenHash(token)
		val tokenEntity = tokenRepository.findByScopeAndHash(scope, tokenHash)
			?: throw TokenNotFoundException("Token not found.")

		if (tokenEntity.issuedAt.plus(tokenEntity.expiresIn).isBefore(Instant.now())) {
			throw TokenExpiredException("Token has expired.")
		}

		return tokenEntity
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
