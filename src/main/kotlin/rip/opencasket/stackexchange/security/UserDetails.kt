package rip.opencasket.stackexchange.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import rip.opencasket.stackexchange.user.UserRepository

class UserDetailsImpl(
	private val username: String,
	private val password: String,
	private val authorities: MutableCollection<out GrantedAuthority>
) : UserDetails {

	override fun getUsername(): String = username

	override fun getPassword(): String = password

	override fun getAuthorities(): MutableCollection<out GrantedAuthority> = authorities

	override fun isAccountNonExpired(): Boolean = true

	override fun isAccountNonLocked(): Boolean = true

	override fun isCredentialsNonExpired(): Boolean = true

	override fun isEnabled(): Boolean = true
}

@Service
class UserDetailsServiceImpl(private val userRepository: UserRepository) : UserDetailsService {
	override fun loadUserByUsername(username: String): UserDetails {
		val user = userRepository.findByUsername(username)
			?: throw UsernameNotFoundException("User $username not found")
		return UserDetailsImpl(user.username, user.passwordHash, mutableListOf())
	}
}