package rip.opencasket.stackexchange.token

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import rip.opencasket.stackexchange.config.SecurityProperties
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
        securityProperties = mock()

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

        val user = User(
            id = 1L,
            username = "user",
            email = email,
            passwordHash = "password"
        )

        val newTokenPlaintext = "newTokenPlaintext"
        val newTokenHash = "newTokenHash"

        // alter the activation token expiry time to validate the default is not hard coded
        val activationTokenExpiry = securityProperties.activationTokenExpiry.plus(Duration.ofHours(4))
        val expectedExpiry = clock.instant().plus(activationTokenExpiry)

        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))
        whenever(tokenGenerator.generateToken()).thenReturn(newTokenPlaintext)
        whenever(tokenGenerator.generateTokenHash(newTokenPlaintext)).thenReturn(newTokenHash)
        whenever(securityProperties.activationTokenExpiry).thenReturn(activationTokenExpiry)
        whenever(tokenRepository.save(any<Token>())).thenAnswer { invocation -> invocation.arguments[0] as Token }

        val result = tokenService.createNewActivationToken(email)

        assertThat(result.isPresent).isTrue()

        val token = result.get()

        assertThat(token.plaintext).isEqualTo(newTokenPlaintext)
        assertThat(token.expiry).isEqualTo(expectedExpiry)

        verify(events).publishEvent(ActivationTokenCreationEvent(email, TokenDto(newTokenPlaintext, expectedExpiry)))
        verify(tokenGenerator).generateToken()
        verify(tokenGenerator).generateTokenHash(newTokenPlaintext)
        verify(tokenRepository).deleteByScopeAndUserId(TokenScope.ACTIVATION, user.id!!)

        val tokenCaptor = ArgumentCaptor.forClass(Token::class.java)
        verify(tokenRepository).save(tokenCaptor.capture())
        val savedToken = tokenCaptor.value
        assertThat(savedToken.hash).isEqualTo(newTokenHash)
        assertThat(savedToken.issuedAt).isEqualTo(clock.instant())
        assertThat(savedToken.expiresIn).isEqualTo(activationTokenExpiry)
        assertThat(savedToken.scope).isEqualTo(TokenScope.ACTIVATION)
        assertThat(savedToken.user).isEqualTo(user)
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
    fun `authenticateUser should return tokens if credentials are valid`() {
    }

    @Test
    fun `authenticateUser should throw InvalidCredentialsException if password is incorrect`() {
    }

    @Test
    fun `refreshAuthenticationToken should return new tokens if refresh token is valid`() {

    }

    @Test
    fun `refreshAuthenticationToken should throw InvalidTokenException if refresh token is invalid`() {
    }
}