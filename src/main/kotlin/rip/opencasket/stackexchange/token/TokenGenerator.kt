package rip.opencasket.stackexchange.token

import com.google.common.hash.Hashing
import org.springframework.stereotype.Component
import rip.opencasket.stackexchange.config.SecurityProperties
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*

interface TokenGenerator {
	fun generateToken(): String
	fun generateTokenHash(plaintext: String): String
}

@Component
class TokenGeneratorImpl(private val securityProperties: SecurityProperties) : TokenGenerator {

	companion object {
		private val secureRandom = SecureRandom()
	}

	override fun generateToken(): String {
		val randomBytes = ByteArray(securityProperties.tokenByteSize)
		secureRandom.nextBytes(randomBytes)
		return Base64.getEncoder().withoutPadding().encodeToString(randomBytes)
	}

	override fun generateTokenHash(plaintext: String): String {
		return Hashing.sha256().hashString(plaintext, StandardCharsets.UTF_8).toString()
	}
}