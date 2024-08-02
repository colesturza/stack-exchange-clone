package rip.opencasket.stackexchange.token

import org.springframework.data.jpa.repository.JpaRepository

interface TokenRepository : JpaRepository<Token, Long> {
	fun findByScopeAndHash(scope: TokenScope, hash: String): Token?
	fun deleteByScopeAndUserId(scope: TokenScope, userId: Long)
}