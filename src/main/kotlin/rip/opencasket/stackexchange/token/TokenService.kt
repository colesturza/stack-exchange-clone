package rip.opencasket.stackexchange.token

import com.google.common.hash.Hashing
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rip.opencasket.stackexchange.user.User
import rip.opencasket.stackexchange.user.UserAlreadyActiveException
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
    private val passwordEncoder: PasswordEncoder,
    private val events: ApplicationEventPublisher
) {

    companion object {
        // Token byte size set to 32 bytes (256 bits) to ensure a high level of security.
        // This will produce a Base64-encoded string of 43 characters.
        private const val TOKEN_BYTE_SIZE = 32
        private val secureRandom = SecureRandom()
    }

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

        val newToken = createToken(user, TokenScope.ACTIVATION, Duration.ofDays(3))

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

        if (user.isActive) {
            throw UserAlreadyActiveException("User account is already active.")
        }

        user.isActive = true
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

        val newToken = createToken(user, TokenScope.PASSWORD_RESET, Duration.ofMinutes(15))

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
     * Authenticates a user with the provided username and password.
     *
     * This method performs the following steps:
     * 1. Finds the user by username.
     * 2. Checks if the provided password matches the stored password hash.
     * 3. Generates and returns a pair of new authentication and refresh tokens for the authenticated user.
     *
     * @param username The username of the user to be authenticated.
     * @param password The password provided by the user for authentication.
     * @return A pair of `TokenDto` representing the authentication and refresh tokens for the authenticated user.
     * @throws UsernameNotFoundException if the user with the given username is not found.
     * @throws InvalidCredentialsException if the provided password is invalid.
     */
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
            tokenRepository.findByScopeAndHash(TokenScope.REFRESH, generateTokenHash(refreshToken)).orElseThrow {
                throw TokenNotFoundException("Refresh token not found or expired.")
            }

        if (tokenEntity.issuedAt.plus(tokenEntity.expiresIn).isBefore(Instant.now())) {
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
            authorities = authorities
        )
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
        tokenRepository.deleteByScopeInAndUserId(listOf(TokenScope.AUTHENTICATION, TokenScope.REFRESH), user.id!!)

        val authToken = createToken(user, TokenScope.AUTHENTICATION, Duration.ofHours(1))
        val refreshToken = createToken(user, TokenScope.REFRESH, Duration.ofDays(30))

        return Pair(authToken, refreshToken)
    }

    private fun validateToken(token: String, scope: TokenScope): Token {
        val tokenHash = generateTokenHash(token)
        val tokenEntity = tokenRepository.findByScopeAndHash(scope, tokenHash).orElseThrow {
            throw TokenNotFoundException("Token not found.")
        }

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
