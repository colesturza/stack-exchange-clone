package rip.opencasket.stackexchange.token

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface TokenRepository : JpaRepository<Token, Long> {
	fun findByScopeAndHash(scope: TokenScope, hash: String): Optional<Token>
	fun deleteByScopeAndUserId(scope: TokenScope, userId: Long)
	fun deleteByScopeInAndUserId(scopes: List<TokenScope>, userId: Long)
}