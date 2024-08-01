package rip.opencasket.stackexchange.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import rip.opencasket.stackexchange.security.AuditorAwareImpl


@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
class PersistenceConfig {

	@Bean
	fun auditorAware(): AuditorAware<Long> {
		return AuditorAwareImpl()
	}
}