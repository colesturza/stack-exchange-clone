package rip.opencasket.stackexchange.token

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.secondValue
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import rip.opencasket.stackexchange.config.SecurityProperties
import rip.opencasket.stackexchange.user.AccountLockedException
import rip.opencasket.stackexchange.user.User
import rip.opencasket.stackexchange.user.UserRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.*


class TokenServiceTests {

	private lateinit var tokenService: TokenService
	private lateinit var tokenGenerator: TokenGenerator
	private lateinit var tokenRepository: TokenRepository
	private lateinit var userRepository: UserRepository
	private lateinit var passwordEncoder: PasswordEncoder
	private lateinit var events: ApplicationEventPublisher
	private lateinit var securityProperties: SecurityProperties

	private val clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"))

	@BeforeEach
	fun setup() {
		tokenGenerator = mock()
		tokenRepository = mock()
		userRepository = mock()
		passwordEncoder = mock()
		events = mock()
		securityProperties = SecurityProperties(
			activationTokenExpiry = Duration.ofDays(4),
			passwordResetTokenExpiry = Duration.ofMinutes(10),
			authTokenExpiry = Duration.ofHours(2),
			refreshTokenExpiry = Duration.ofDays(2),
			accountLockDuration = Duration.ofMinutes(5),
			maxFailedLoginAttempts = 3
		)

		tokenService = TokenService(
			securityProperties,
			tokenGenerator,
			tokenRepository,
			userRepository,
			passwordEncoder,
			clock,
			events
		)
	}

	@Test
	fun `createNewActivationToken should return a token if user exists`() {
		val email = "user@example.com"
		val newTokenPlaintext = "newTokenPlaintext"
		val newTokenHash = "newTokenHash"

		val initialUser = User(
			id = 1L,
			username = "user",
			email = email,
			passwordHash = "password"
		)

		val expectedUser = User(
			id = 1L,
			username = "user",
			email = email,
			passwordHash = "password"
		)

		val expectedExpiry = clock.instant().plus(securityProperties.activationTokenExpiry)

		whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(initialUser))
		whenever(tokenGenerator.generateToken()).thenReturn(newTokenPlaintext)
		whenever(tokenGenerator.generateTokenHash(newTokenPlaintext)).thenReturn(newTokenHash)
		whenever(tokenRepository.save(any<Token>())).thenAnswer { invocation -> invocation.arguments[0] as Token }

		val result = tokenService.createNewActivationToken(email)

		assertThat(result.isPresent).isTrue()

		val token = result.get()

		assertThat(token.plaintext).isEqualTo(newTokenPlaintext)
		assertThat(token.expiry).isEqualTo(expectedExpiry)

		verify(events).publishEvent(ActivationTokenCreationEvent(email, TokenDto(newTokenPlaintext, expectedExpiry)))
		verify(tokenGenerator).generateToken()
		verify(tokenGenerator).generateTokenHash(newTokenPlaintext)
		verify(tokenRepository).deleteByScopeAndUserId(TokenScope.ACTIVATION, expectedUser.id!!)

		val tokenCaptor = ArgumentCaptor.forClass(Token::class.java)
		verify(tokenRepository).save(tokenCaptor.capture())
		val savedToken = tokenCaptor.value
		assertThat(savedToken.hash).isEqualTo(newTokenHash)
		assertThat(savedToken.issuedAt).isEqualTo(clock.instant())
		assertThat(savedToken.expiresIn).isEqualTo(securityProperties.activationTokenExpiry)
		assertThat(savedToken.scope).isEqualTo(TokenScope.ACTIVATION)
		assertThat(savedToken.user).isEqualTo(expectedUser)
	}

	@Test
	fun `createNewActivationToken should return empty if user does not exist`() {
		val email = "doesnotexist@example.com"

		whenever(userRepository.findByEmail(email)).thenReturn(Optional.empty())

		val result = tokenService.createNewActivationToken(email)

		assertThat(result.isPresent).isFalse()

		verify(events, never()).publishEvent(any())
		verify(tokenGenerator, never()).generateToken()
		verify(tokenGenerator, never()).generateTokenHash(any())
		verify(tokenRepository, never()).deleteByScopeAndUserId(any(), any())
		verify(tokenRepository, never()).save(any())
	}

	@Test
	fun `activateUserAccount should activate user if token is valid`() {

	}

	@Test
	fun `activateUserAccount should throw UserAlreadyActiveException if user is already active`() {
	}

	@Test
	fun `createNewPasswordResetToken should return new token if user exists`() {
	}

	@Test
	fun `createNewPasswordResetToken should return empty if user does not exist`() {
	}

	@Test
	fun `resetUserPassword should update password if token is valid`() {
	}

	@Test
	fun `refreshAuthenticationToken should return new tokens if refresh token is valid`() {

	}

	@Test
	fun `refreshAuthenticationToken should throw InvalidTokenException if refresh token is invalid`() {
	}

	@Test
	fun `authenticateUser should return tokens and not update failed attempts if credentials are valid`() {
		val username = "user"
		val password = "password"
		val passwordHash = "hashedPassword"
		val authToken = "authToken"
		val authTokenHash = "hashedAuthToken"
		val refreshToken = "refreshToken"
		val refreshTokenHash = "hashedRefreshToken"

		val initialUser = User(
			id = 1L,
			username = username,
			email = "user@example.com",
			passwordHash = passwordHash,
			failedLoginAttempts = 0,
			lockedAt = null,
			lockedDuration = null
		)

		val expectedUser = User(
			id = 1L,
			username = username,
			email = "user@example.com",
			passwordHash = passwordHash,
			failedLoginAttempts = 0,
			lockedAt = null,
			lockedDuration = null
		)

		val expectedAuthTokenExpiry = clock.instant().plus(securityProperties.authTokenExpiry)
		val expectedRefreshTokenExpiry = clock.instant().plus(securityProperties.refreshTokenExpiry)

		whenever(userRepository.findByUsername(username)).thenReturn(Optional.of(initialUser))
		whenever(passwordEncoder.matches(password, passwordHash)).thenReturn(true)
		whenever(tokenGenerator.generateToken()).thenReturn(authToken, refreshToken)
		whenever(tokenGenerator.generateTokenHash(authToken)).thenReturn(authTokenHash)
		whenever(tokenGenerator.generateTokenHash(refreshToken)).thenReturn(refreshTokenHash)
		whenever(tokenRepository.save(any<Token>())).thenAnswer { invocation -> invocation.arguments[0] as Token }

		val result = tokenService.authenticateUser(username, password)

		assertThat(result.first.plaintext).isEqualTo(authToken)
		assertThat(result.first.expiry).isEqualTo(expectedAuthTokenExpiry)
		assertThat(result.second.plaintext).isEqualTo(refreshToken)
		assertThat(result.second.expiry).isEqualTo(expectedRefreshTokenExpiry)

		// should not be evoked since the user has no failed login attempts
		verify(userRepository, never()).save(any())

		val tokenCaptor = ArgumentCaptor.forClass(Token::class.java)
		verify(tokenRepository, times(2)).save(tokenCaptor.capture())

		val savedAuthToken = tokenCaptor.firstValue
		assertThat(savedAuthToken.hash).isEqualTo(authTokenHash)
		assertThat(savedAuthToken.issuedAt).isEqualTo(clock.instant())
		assertThat(savedAuthToken.expiresIn).isEqualTo(securityProperties.authTokenExpiry)
		assertThat(savedAuthToken.scope).isEqualTo(TokenScope.AUTHENTICATION)
		assertThat(savedAuthToken.user).isEqualTo(expectedUser)

		val savedRefreshToken = tokenCaptor.secondValue
		assertThat(savedRefreshToken.hash).isEqualTo(refreshTokenHash)
		assertThat(savedRefreshToken.issuedAt).isEqualTo(clock.instant())
		assertThat(savedRefreshToken.expiresIn).isEqualTo(securityProperties.refreshTokenExpiry)
		assertThat(savedRefreshToken.scope).isEqualTo(TokenScope.REFRESH)
		assertThat(savedRefreshToken.user).isEqualTo(expectedUser)
	}

	@Test
	fun `authenticateUser should return tokens and update failed attempts if credentials are valid`() {
		val username = "user"
		val password = "password"
		val passwordHash = "hashedPassword"
		val authToken = "authToken"
		val authTokenHash = "hashedAuthToken"
		val refreshToken = "refreshToken"
		val refreshTokenHash = "hashedRefreshToken"

		val initialUser = User(
			id = 1L,
			username = username,
			email = "user@example.com",
			passwordHash = passwordHash,
			failedLoginAttempts = securityProperties.maxFailedLoginAttempts - 1,
			lockedAt = null,
			lockedDuration = null
		)

		val expectedUser = User(
			id = 1L,
			username = username,
			email = "user@example.com",
			passwordHash = passwordHash,
			failedLoginAttempts = 0,
			lockedAt = null,
			lockedDuration = null
		)

		val expectedAuthTokenExpiry = clock.instant().plus(securityProperties.authTokenExpiry)
		val expectedRefreshTokenExpiry = clock.instant().plus(securityProperties.refreshTokenExpiry)

		whenever(userRepository.findByUsername(username)).thenReturn(Optional.of(initialUser))
		whenever(userRepository.save(any<User>())).thenAnswer { invocation -> invocation.arguments[0] as User }
		whenever(passwordEncoder.matches(password, passwordHash)).thenReturn(true)
		whenever(tokenGenerator.generateToken()).thenReturn(authToken, refreshToken)
		whenever(tokenGenerator.generateTokenHash(authToken)).thenReturn(authTokenHash)
		whenever(tokenGenerator.generateTokenHash(refreshToken)).thenReturn(refreshTokenHash)
		whenever(tokenRepository.save(any<Token>())).thenAnswer { invocation -> invocation.arguments[0] as Token }

		val result = tokenService.authenticateUser(username, password)

		assertThat(result.first.plaintext).isEqualTo(authToken)
		assertThat(result.first.expiry).isEqualTo(expectedAuthTokenExpiry)
		assertThat(result.second.plaintext).isEqualTo(refreshToken)
		assertThat(result.second.expiry).isEqualTo(expectedRefreshTokenExpiry)

		val userCaptor = ArgumentCaptor.forClass(User::class.java)
		verify(userRepository, times(1)).save(userCaptor.capture())
		val savedUser = userCaptor.value
		assertThat(savedUser.failedLoginAttempts).isEqualTo(0)
		assertThat(savedUser.lockedAt).isNull()
		assertThat(savedUser.lockedDuration).isNull()

		val tokenCaptor = ArgumentCaptor.forClass(Token::class.java)
		verify(tokenRepository, times(2)).save(tokenCaptor.capture())

		val savedAuthToken = tokenCaptor.firstValue
		assertThat(savedAuthToken.hash).isEqualTo(authTokenHash)
		assertThat(savedAuthToken.issuedAt).isEqualTo(clock.instant())
		assertThat(savedAuthToken.expiresIn).isEqualTo(securityProperties.authTokenExpiry)
		assertThat(savedAuthToken.scope).isEqualTo(TokenScope.AUTHENTICATION)
		assertThat(savedAuthToken.user).isEqualTo(expectedUser)

		val savedRefreshToken = tokenCaptor.secondValue
		assertThat(savedRefreshToken.hash).isEqualTo(refreshTokenHash)
		assertThat(savedRefreshToken.issuedAt).isEqualTo(clock.instant())
		assertThat(savedRefreshToken.expiresIn).isEqualTo(securityProperties.refreshTokenExpiry)
		assertThat(savedRefreshToken.scope).isEqualTo(TokenScope.REFRESH)
		assertThat(savedRefreshToken.user).isEqualTo(expectedUser)
	}

	@Test
	fun `authenticateUser should throw UsernameNotFoundException if user does not exist`() {
		val username = "nonexistent"
		val password = "password"

		whenever(userRepository.findByUsername(username)).thenReturn(Optional.empty())

		assertThatExceptionOfType(UsernameNotFoundException::class.java).isThrownBy {
			tokenService.authenticateUser(
				username,
				password
			)
		}.withMessage("User with username '$username' not found.")

		verify(passwordEncoder, never()).matches(any(), any())
		verify(tokenGenerator, never()).generateToken()
		verify(tokenGenerator, never()).generateTokenHash(any())
		verify(userRepository, never()).save(any<User>())
		verify(tokenRepository, never()).save(any<Token>())
	}

	@Test
	fun `authenticateUser should throw InvalidCredentialsException if password is incorrect`() {
		val username = "user"
		val password = "wrongPassword"
		val passwordHash = "hashedPassword"

		whenever(userRepository.findByUsername(username)).thenReturn(
			Optional.of(
				User(
					id = 1L,
					username = username,
					email = "user@example.com",
					passwordHash = passwordHash,
					failedLoginAttempts = 0
				)
			)
		)
		whenever(userRepository.save(any<User>())).thenAnswer { invocation -> invocation.arguments[0] as User }
		whenever(passwordEncoder.matches(password, passwordHash)).thenReturn(false)

		assertThatExceptionOfType(InvalidCredentialsException::class.java).isThrownBy {
			tokenService.authenticateUser(
				username,
				password
			)
		}.withMessage("Invalid password.")

		verify(tokenGenerator, never()).generateToken()
		verify(tokenGenerator, never()).generateTokenHash(any())
		verify(tokenRepository, never()).save(any<Token>())

		val userCaptor = ArgumentCaptor.forClass(User::class.java)
		verify(userRepository, times(1)).save(userCaptor.capture())
		val savedUser = userCaptor.value
		assertThat(savedUser.failedLoginAttempts).isEqualTo(1)
		assertThat(savedUser.lockedAt).isNull()
		assertThat(savedUser.lockedDuration).isNull()
	}

	@Test
	fun `authenticateUser should throw AccountLockedException if account is locked`() {
		val username = "lockedUser"
		val password = "password"
		val passwordHash = "hashedPassword"
		val lockedAt = clock.instant().minus(Duration.ofHours(1))
		val lockDuration = Duration.ofHours(2)

		whenever(userRepository.findByUsername(username)).thenReturn(
			Optional.of(
				User(
					id = 1L,
					username = username,
					email = "user@example.com",
					passwordHash = passwordHash,
					failedLoginAttempts = 0,
					lockedAt = lockedAt,
					lockedDuration = lockDuration
				)
			)
		)

		assertThatExceptionOfType(AccountLockedException::class.java).isThrownBy {
			tokenService.authenticateUser(
				username,
				password
			)
		}.withMessage("Account is locked. Try again later.")

		verify(passwordEncoder, never()).matches(any(), any())
		verify(tokenGenerator, never()).generateToken()
		verify(tokenGenerator, never()).generateTokenHash(any())
		verify(userRepository, never()).save(any<User>())
		verify(tokenRepository, never()).save(any<Token>())
	}

	@Test
	fun `authenticateUser should reset lock status if lock duration has expired and return return tokens`() {
		val username = "user"
		val password = "password"
		val passwordHash = "hashedPassword"
		val authToken = "authToken"
		val authTokenHash = "hashedAuthToken"
		val refreshToken = "refreshToken"
		val refreshTokenHash = "hashedRefreshToken"
		val lockedAt = clock.instant().minus(Duration.ofHours(3))
		val lockDuration = Duration.ofHours(2)

		val initialUser = User(
			id = 1L,
			username = username,
			email = "user@example.com",
			passwordHash = passwordHash,
			failedLoginAttempts = securityProperties.maxFailedLoginAttempts,
			lockedAt = lockedAt,
			lockedDuration = lockDuration
		)

		val expectedUser = User(
			id = 1L,
			username = username,
			email = "user@example.com",
			passwordHash = passwordHash,
			failedLoginAttempts = 0,
			lockedAt = null,
			lockedDuration = null
		)

		val expectedAuthTokenExpiry = clock.instant().plus(securityProperties.authTokenExpiry)
		val expectedRefreshTokenExpiry = clock.instant().plus(securityProperties.refreshTokenExpiry)

		whenever(userRepository.findByUsername(username)).thenReturn(Optional.of(initialUser))
		whenever(userRepository.save(any<User>())).thenAnswer { invocation -> invocation.arguments[0] as User }
		whenever(passwordEncoder.matches(password, passwordHash)).thenReturn(true)
		whenever(tokenGenerator.generateToken()).thenReturn(authToken, refreshToken)
		whenever(tokenGenerator.generateTokenHash(authToken)).thenReturn(authTokenHash)
		whenever(tokenGenerator.generateTokenHash(refreshToken)).thenReturn(refreshTokenHash)
		whenever(tokenRepository.save(any<Token>())).thenAnswer { invocation -> invocation.arguments[0] as Token }

		val result = tokenService.authenticateUser(username, password)

		assertThat(result.first.plaintext).isEqualTo(authToken)
		assertThat(result.first.expiry).isEqualTo(expectedAuthTokenExpiry)
		assertThat(result.second.plaintext).isEqualTo(refreshToken)
		assertThat(result.second.expiry).isEqualTo(expectedRefreshTokenExpiry)

		val userCaptor = ArgumentCaptor.forClass(User::class.java)
		verify(userRepository, times(1)).save(userCaptor.capture())
		val savedUser = userCaptor.value
		assertThat(savedUser.failedLoginAttempts).isEqualTo(0)
		assertThat(savedUser.lockedAt).isNull()
		assertThat(savedUser.lockedDuration).isNull()

		val tokenCaptor = ArgumentCaptor.forClass(Token::class.java)
		verify(tokenRepository, times(2)).save(tokenCaptor.capture())

		val savedAuthToken = tokenCaptor.firstValue
		assertThat(savedAuthToken.hash).isEqualTo(authTokenHash)
		assertThat(savedAuthToken.issuedAt).isEqualTo(clock.instant())
		assertThat(savedAuthToken.expiresIn).isEqualTo(securityProperties.authTokenExpiry)
		assertThat(savedAuthToken.scope).isEqualTo(TokenScope.AUTHENTICATION)
		assertThat(savedAuthToken.user).isEqualTo(expectedUser)

		val savedRefreshToken = tokenCaptor.secondValue
		assertThat(savedRefreshToken.hash).isEqualTo(refreshTokenHash)
		assertThat(savedRefreshToken.issuedAt).isEqualTo(clock.instant())
		assertThat(savedRefreshToken.expiresIn).isEqualTo(securityProperties.refreshTokenExpiry)
		assertThat(savedRefreshToken.scope).isEqualTo(TokenScope.REFRESH)
		assertThat(savedRefreshToken.user).isEqualTo(expectedUser)
	}

	@Test
	fun `authenticateUser should reset lock status if lock duration has expired and not return return tokens`() {
		val username = "user"
		val password = "wrongPassword"
		val passwordHash = "hashedPassword"
		val lockedAt = clock.instant().minus(Duration.ofHours(3))
		val lockDuration = Duration.ofHours(2)

		whenever(userRepository.findByUsername(username)).thenReturn(
			Optional.of(
				User(
					id = 1L,
					username = username,
					email = "user@example.com",
					passwordHash = passwordHash,
					failedLoginAttempts = securityProperties.maxFailedLoginAttempts,
					lockedAt = lockedAt,
					lockedDuration = lockDuration
				)
			)
		)
		whenever(passwordEncoder.matches(password, passwordHash)).thenReturn(false)

		assertThatExceptionOfType(InvalidCredentialsException::class.java).isThrownBy {
			tokenService.authenticateUser(
				username,
				password
			)
		}.withMessage("Invalid password.")

		verify(tokenGenerator, never()).generateToken()
		verify(tokenGenerator, never()).generateTokenHash(any())
		verify(tokenRepository, never()).save(any<Token>())

		val userCaptor = ArgumentCaptor.forClass(User::class.java)
		verify(userRepository, times(1)).save(userCaptor.capture())
		val savedUser = userCaptor.value
		assertThat(savedUser.failedLoginAttempts).isEqualTo(1)
		assertThat(savedUser.lockedAt).isNull()
		assertThat(savedUser.lockedDuration).isNull()
	}

	@Test
	fun `authenticateUser should lock account after failed login attempt`() {
		val username = "user"
		val password = "wrongPassword"
		val passwordHash = "hashedPassword"

		whenever(userRepository.findByUsername(username)).thenReturn(
			Optional.of(
				User(
					id = 1L,
					username = username,
					email = "user@example.com",
					passwordHash = passwordHash,
					failedLoginAttempts = securityProperties.maxFailedLoginAttempts - 1,
				)
			)
		)
		whenever(passwordEncoder.matches(password, passwordHash)).thenReturn(false)

		assertThatExceptionOfType(InvalidCredentialsException::class.java).isThrownBy {
			tokenService.authenticateUser(
				username,
				password
			)
		}.withMessage("Invalid password.")

		verify(tokenGenerator, never()).generateToken()
		verify(tokenGenerator, never()).generateTokenHash(any())
		verify(tokenRepository, never()).save(any<Token>())

		val userCaptor = ArgumentCaptor.forClass(User::class.java)
		verify(userRepository).save(userCaptor.capture())
		val savedUser = userCaptor.value
		assertThat(savedUser.failedLoginAttempts).isEqualTo(securityProperties.maxFailedLoginAttempts)
		assertThat(savedUser.lockedAt).isEqualTo(clock.instant())
		assertThat(savedUser.lockedDuration).isEqualTo(securityProperties.accountLockDuration)
	}
}