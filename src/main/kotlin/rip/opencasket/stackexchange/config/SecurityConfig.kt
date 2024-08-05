package rip.opencasket.stackexchange.config

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.ProviderManager
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
import rip.opencasket.stackexchange.security.BearerTokenAccessDeniedHandler
import rip.opencasket.stackexchange.security.BearerTokenAuthenticationEntryPoint
import rip.opencasket.stackexchange.security.BearerTokenAuthenticationFilter
import rip.opencasket.stackexchange.security.BearerTokenAuthenticationProvider
import rip.opencasket.stackexchange.token.TokenGenerator
import rip.opencasket.stackexchange.token.TokenRepository
import rip.opencasket.stackexchange.token.TokenService
import rip.opencasket.stackexchange.user.UserRepository
import java.time.Clock


@Configuration
@EnableWebSecurity
class SecurityConfig(
	private val securityProperties: SecurityProperties
) {

	@Bean
	fun passwordEncoder(): PasswordEncoder {
		val defaultEncoder = "bcrypt"
		val encoders = mutableMapOf<String, PasswordEncoder>(
			defaultEncoder to BCryptPasswordEncoder(securityProperties.bcryptStrength)
		)
		return DelegatingPasswordEncoder(defaultEncoder, encoders)
	}

	@Bean
	fun tokenService(
		tokenGenerator: TokenGenerator,
		tokenRepository: TokenRepository,
		userRepository: UserRepository,
		passwordEncoder: PasswordEncoder,
		clock: Clock,
		events: ApplicationEventPublisher
	): TokenService {
		return TokenService(
			securityProperties,
			tokenGenerator,
			tokenRepository,
			userRepository,
			passwordEncoder,
			clock,
			events
		)
	}

	@Bean
	fun filterChain(
		http: HttpSecurity,
		tokenService: TokenService,
		bearerTokenAccessDeniedHandler: BearerTokenAccessDeniedHandler,
		bearerTokenAuthenticationEntryPoint: BearerTokenAuthenticationEntryPoint
	): SecurityFilterChain {

		val authenticationManager = ProviderManager(
			BearerTokenAuthenticationProvider(tokenService)
		)

		http.authenticationManager(authenticationManager)
		http {
			csrf {
				// CORs will be configured to only allow requests from frontend host
				// shouldn't need CSRF tokens in that case
				disable()
			}
			formLogin {
				disable()
			}
			httpBasic {
				disable()
			}
			exceptionHandling {
				accessDeniedHandler = bearerTokenAccessDeniedHandler
				authenticationEntryPoint = bearerTokenAuthenticationEntryPoint
			}
			authorizeHttpRequests {
				authorize(antMatcher(HttpMethod.POST, "/users"), permitAll)
				authorize(antMatcher(HttpMethod.POST, "/tokens/authentication"), permitAll)
				authorize(antMatcher(HttpMethod.POST, "/tokens/refresh"), permitAll)
				authorize(antMatcher(HttpMethod.POST, "/tokens/activation/new"), permitAll)
				authorize(antMatcher(HttpMethod.POST, "/tokens/activation"), permitAll)
				authorize(antMatcher(HttpMethod.POST, "/tokens/password-reset/new"), permitAll)
				authorize(antMatcher(HttpMethod.POST, "/tokens/password-reset"), permitAll)
				authorize(anyRequest, authenticated)
			}
			sessionManagement {
				sessionCreationPolicy = SessionCreationPolicy.STATELESS
			}
			addFilterBefore<UsernamePasswordAuthenticationFilter>(BearerTokenAuthenticationFilter(authenticationManager))
		}

		return http.build()
	}
}
