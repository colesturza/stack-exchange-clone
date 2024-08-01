package rip.opencasket.stackexchange.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher
import rip.opencasket.stackexchange.security.BearerTokenAuthenticationFilter
import rip.opencasket.stackexchange.token.TokenRepository
import rip.opencasket.stackexchange.token.TokenService
import rip.opencasket.stackexchange.user.UserRepository

@Configuration
@EnableWebSecurity
class SecurityConfig(private val userRepository: UserRepository, private val tokenRepository: TokenRepository) {

	@Bean
	fun passwordEncoder(): PasswordEncoder {
		val defaultEncoder = "bcrypt"
		val encoders = mutableMapOf<String, PasswordEncoder>(
			defaultEncoder to BCryptPasswordEncoder()
		)
		return DelegatingPasswordEncoder(defaultEncoder, encoders)
	}

	@Bean
	fun tokenService(): TokenService {
		return TokenService(tokenRepository, userRepository, passwordEncoder())
	}

	@Bean
	fun filterChain(http: HttpSecurity): SecurityFilterChain {
		http {
			csrf {
				disable()
			}
			authorizeHttpRequests {
				authorize(antMatcher(HttpMethod.POST, "/users"), permitAll)
				authorize(antMatcher(HttpMethod.POST, "/tokens/authentication"), permitAll)
				authorize(anyRequest, authenticated)
			}
			sessionManagement {
				sessionCreationPolicy = SessionCreationPolicy.STATELESS
			}
			addFilterBefore<UsernamePasswordAuthenticationFilter>(BearerTokenAuthenticationFilter(tokenService()))
		}
		return http.build()
	}
}
