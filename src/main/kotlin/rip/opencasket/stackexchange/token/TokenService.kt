package rip.opencasket.stackexchange.token

import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rip.opencasket.stackexchange.config.SecurityProperties
import rip.opencasket.stackexchange.user.AccountLockedException
import rip.opencasket.stackexchange.user.User
import rip.opencasket.stackexchange.user.UserAlreadyActiveException
import rip.opencasket.stackexchange.user.UserAuthoritiesDto
import rip.opencasket.stackexchange.user.UserRepository
import java.time.Clock
import java.time.Duration
import java.util.*

@Service
class TokenService(
	private val securityProperties: SecurityProperties,
	private val tokenGenerator: TokenGenerator,
	private val tokenRepository: TokenRepository,
	private val userRepository: UserRepository,
	private val passwordEncoder: PasswordEncoder,
	private val clock: Clock,
	private val events: ApplicationEventPublisher
) {

	/**
	 * Creates a new activation token for the user with the specified email address.
	 *
	 * This method performs the following steps:
	 * 1. Attempts to find the user associated with the given email address.
	 * 2. If the user is found, all previous activation tokens for this user are removed.
	 * 3. A new activation token is generated and returned.
	 *
	 * If the user with the given email address is not found, the method will silently not work and
	 * return an empty `Optional`. This behavior ensures that no information is disclosed about
	 * whether an email address exists in the database, in order to maintain security and privacy.
	 *
	 * @param email The email address of the user for whom the activation token is to be created.
	 * @return An `Optional` containing the newly created activation token if the user is found,
	 *         or an empty `Optional` if the user is not found.
	 */
	@Transactional
	fun createNewActivationToken(email: String): Optional<TokenDto> {
		val user = userRepository.findByEmail(email).orElse(null) ?: return Optional.empty()

		tokenRepository.deleteByScopeAndUserId(TokenScope.ACTIVATION, user.id!!)

		val newToken = createToken(user, TokenScope.ACTIVATION, securityProperties.activationTokenExpiry)

		events.publishEvent(ActivationTokenCreationEvent(email, newToken))

		return Optional.of(newToken)
	}

	/**
	 * Activates the user account associated with the given activation token.
	 *
	 * This method performs the following steps:
	 * 1. Validates the activation token.
	 * 2. Checks if the user account is already active. If so, throws `UserAlreadyActiveException`.
	 * 3. Sets the user's account to active and saves the changes.
	 * 4. Removes all activation tokens for the user.
	 *
	 * @param token The activation token to be validated and used for activating the user account.
	 * @throws UserAlreadyActiveException if the user account is already active.
	 */
	@Transactional
	fun activateUserAccount(token: String) {
		val tokenEntity = validateToken(token, TokenScope.ACTIVATION)

		val user = tokenEntity.user

		if (user.isEmailVerified) {
			throw UserAlreadyActiveException("User account is already active.")
		}

		user.isEmailVerified = true
		userRepository.save(user)

		tokenRepository.deleteByScopeAndUserId(TokenScope.ACTIVATION, user.id!!)
	}

	/**
	 * Creates a new password reset token for the user with the specified email address.
	 *
	 * This method performs the following steps:
	 * 1. Attempts to find the user associated with the given email address.
	 * 2. If the user is found, all previous password reset tokens for this user are removed.
	 * 3. A new password reset token is generated and returned.
	 *
	 * If the user with the given email address is not found, the method will silently return an empty `Optional`.
	 * This behavior ensures that no information is disclosed about whether an email address exists in the database,
	 * maintaining security and privacy.
	 *
	 * @param email The email address of the user for whom the password reset token is to be created.
	 * @return An `Optional` containing the newly created password reset token if the user is found,
	 *         or an empty `Optional` if the user is not found.
	 */
	@Transactional
	fun createNewPasswordResetToken(email: String): Optional<TokenDto> {
		val user = userRepository.findByEmail(email).orElse(null) ?: return Optional.empty()

		tokenRepository.deleteByScopeAndUserId(TokenScope.PASSWORD_RESET, user.id!!)

		val newToken = createToken(user, TokenScope.PASSWORD_RESET, securityProperties.passwordResetTokenExpiry)

		events.publishEvent(PasswordResetTokenCreationEvent(email, newToken))

		return Optional.of(newToken)
	}

	/**
	 * Resets the user password using the provided password reset token and new password.
	 *
	 * This method performs the following steps:
	 * 1. Validates the password reset token.
	 * 2. Updates the user's password with the new password after encoding it.
	 * 3. Removes all password reset, authentication, and refresh tokens for the user.
	 *
	 * @param token The password reset token used to validate the reset request.
	 * @param newPassword The new password to be set for the user.
	 */
	@Transactional
	fun resetUserPassword(token: String, newPassword: String) {
		val tokenEntity = validateToken(token, TokenScope.PASSWORD_RESET)

		val user = tokenEntity.user

		user.passwordHash = passwordEncoder.encode(newPassword)
		userRepository.save(user)

		tokenRepository.deleteByScopeInAndUserId(
			listOf(
				TokenScope.PASSWORD_RESET,
				TokenScope.AUTHENTICATION,
				TokenScope.REFRESH
			), user.id!!
		)
	}

	/**
	 * Authenticates a user based on their username and password.
	 *
	 * The authentication process includes:
	 * 1. Retrieving the user by their username.
	 * 2. Checking if the account is locked and whether the lock period has expired.
	 * 3. Validating the provided password against the stored password hash.
	 * 4. Resetting the account lock status if necessary.
	 * 5. Generating and returning a pair of authentication and refresh tokens for the authenticated user.
	 *
	 * The method ensures that the account lock status is correctly handled and updates the user entity only when necessary
	 * to minimize database writes. The transaction will not be rolled back in case of an `InvalidCredentialsException`.
	 *
	 * @param username The username of the user to authenticate.
	 * @param password The password provided by the user.
	 * @return A pair of `TokenDto` representing the authentication and refresh tokens for the authenticated user.
	 * @throws UsernameNotFoundException If no user is found with the given username.
	 * @throws InvalidCredentialsException If the provided password does not match the stored password.
	 * @throws AccountLockedException If the account is currently locked and the lock period has not expired.
	 */
	@Transactional(noRollbackFor = [InvalidCredentialsException::class])
	fun authenticateUser(username: String, password: String): Pair<TokenDto, TokenDto> {
		val user = userRepository.findByUsername(username).orElseThrow {
			UsernameNotFoundException("User with username '$username' not found.")
		}

		var updated = handleAccountLocking(user)
		validatePassword(password, user)
		updated = updated or resetUserAccountLockStatus(user)

		if (updated) {
			userRepository.save(user)
		}

		return generateAndSaveNewAuthenticationTokens(user)
	}

	/**
	 * Handles account locking based on the user's lock status.
	 *
	 * This method checks if the user's account is currently locked by evaluating the lock timestamp and duration.
	 * - If the account is locked and the lock period has not expired, an `AccountLockedException` is thrown.
	 * - If the lock period has expired, the account lock status is reset.
	 *
	 * The method returns a boolean indicating whether the account lock status was reset.
	 *
	 * @param user The user whose account lock status is to be checked.
	 * @return `true` if the account lock status was reset; `false` otherwise.
	 * @throws AccountLockedException If the account is currently locked and the lock period has not expired.
	 */
	private fun handleAccountLocking(user: User): Boolean {
		val lockedAt = user.lockedAt
		val lockedDuration = user.lockedDuration

		if (lockedAt != null && lockedDuration != null) {
			val now = clock.instant()
			val lockExpiry = lockedAt.plus(lockedDuration)

			if (now.isBefore(lockExpiry)) {
				throw AccountLockedException("Account is locked. Try again later.")
			}

			return resetUserAccountLockStatus(user)
		}

		return false // No changes were made
	}

	/**
	 * Validates the provided password against the stored password hash.
	 *
	 * This method verifies the provided password by comparing it with the stored hash. If the password does not match,
	 * the user's failed login attempts counter is incremented. If the number of failed attempts reaches a certain
	 * threshold (e.g., 5), the account is locked for a specified duration.
	 *
	 * The method does not return a value as it updates the user's failed login attempts and potentially the account lock status.
	 * If the password does not match, an `InvalidCredentialsException` is thrown.
	 *
	 * @param password The password provided by the user.
	 * @param user The user whose stored password hash is to be checked.
	 * @throws InvalidCredentialsException If the provided password does not match the stored hash.
	 */
	private fun validatePassword(password: String, user: User) {
		if (!passwordEncoder.matches(password, user.passwordHash)) {
			user.failedLoginAttempts++

			if (user.failedLoginAttempts >= securityProperties.maxFailedLoginAttempts) {
				user.lockedAt = clock.instant()
				user.lockedDuration = securityProperties.accountLockDuration
			}

			userRepository.save(user)
			throw InvalidCredentialsException("Invalid password.")
		}
	}

	/**
	 * Resets the user's failed login attempts and unlocks the account if necessary.
	 *
	 * This method resets the user's failed login attempts counter and clears any account lock status (lock timestamp and duration)
	 * if they were previously set. It is called when a user successfully authenticates or when the lock period has expired.
	 *
	 * The method returns a boolean indicating whether the user's account status was reset.
	 *
	 * @param user The user whose account is to be reset.
	 * @return `true` if the account lock status or failed login attempts were reset; `false` otherwise.
	 */
	private fun resetUserAccountLockStatus(user: User): Boolean {
		if (user.failedLoginAttempts > 0 || user.lockedAt != null || user.lockedDuration != null) {
			user.failedLoginAttempts = 0
			user.lockedAt = null
			user.lockedDuration = null
			return true // Indicate that changes were made
		}
		return false // No changes were made
	}

	/**
	 * Refreshes the authentication token using the provided refresh token.
	 *
	 * This method performs the following steps:
	 * 1. Validates the refresh token.
	 * 2. Checks if the token has expired.
	 * 3. Generates and returns a pair of new authentication and refresh tokens.
	 *
	 * @param refreshToken The refresh token used to generate new authentication and refresh tokens.
	 * @return A pair of `TokenDto` representing the new authentication and refresh tokens.
	 * @throws TokenNotFoundException if the refresh token is not found or expired.
	 * @throws TokenExpiredException if the refresh token has expired.
	 */
	@Transactional
	fun refreshAuthenticationToken(refreshToken: String): Pair<TokenDto, TokenDto> {
		val tokenEntity =
			tokenRepository.findByScopeAndHash(TokenScope.REFRESH, tokenGenerator.generateTokenHash(refreshToken))
				.orElseThrow {
					throw TokenNotFoundException("Refresh token not found or expired.")
				}

		if (tokenEntity.issuedAt.plus(tokenEntity.expiresIn).isBefore(clock.instant())) {
			throw TokenExpiredException("Refresh token has expired.")
		}

		val user = tokenEntity.user

		return generateAndSaveNewAuthenticationTokens(user)
	}

	/**
	 * Removes all authentication and refresh tokens associated with the specified user.
	 *
	 * @param userId The ID of the user whose tokens are to be removed.
	 */
	@Transactional
	fun unAuthenticateUser(userId: Long) {
		tokenRepository.deleteByScopeInAndUserId(listOf(TokenScope.AUTHENTICATION, TokenScope.REFRESH), userId)
	}

	/**
	 * Finds user details by token scope and token value.
	 *
	 * This method performs the following steps:
	 * 1. Validates the token.
	 * 2. Retrieves user details and authorities.
	 *
	 * @param scope The scope of the token.
	 * @param token The token value used to find the user.
	 * @return A `UserAuthoritiesDto` containing user details and authorities.
	 */
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
			authorities = authorities,
			isEmailVerified = user.isEmailVerified,
		)
	}

	private fun createToken(user: User, scope: TokenScope, expiresIn: Duration): TokenDto {
		val plaintext = tokenGenerator.generateToken()
		val issuedAt = clock.instant()

		val token = Token(
			hash = tokenGenerator.generateTokenHash(plaintext),
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
		tokenRepository.deleteByScopeInAndUserId(listOf(TokenScope.AUTHENTICATION, TokenScope.REFRESH), user.id!!)

		val authToken = createToken(user, TokenScope.AUTHENTICATION, securityProperties.authTokenExpiry)
		val refreshToken = createToken(user, TokenScope.REFRESH, securityProperties.refreshTokenExpiry)

		return Pair(authToken, refreshToken)
	}

	private fun validateToken(token: String, scope: TokenScope): Token {
		val tokenHash = tokenGenerator.generateTokenHash(token)
		val tokenEntity = tokenRepository.findByScopeAndHash(scope, tokenHash).orElseThrow {
			throw TokenNotFoundException("Token not found.")
		}

		if (tokenEntity.issuedAt.plus(tokenEntity.expiresIn).isBefore(clock.instant())) {
			throw TokenExpiredException("Token has expired.")
		}

		return tokenEntity
	}
}
