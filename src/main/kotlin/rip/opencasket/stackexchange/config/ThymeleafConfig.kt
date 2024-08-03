package rip.opencasket.stackexchange.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver
import org.thymeleaf.templatemode.TemplateMode

@Configuration
class ThymeleafConfig {

	@Bean
	fun htmlTemplateResolver(): SpringResourceTemplateResolver {
		val resolver = SpringResourceTemplateResolver()
		resolver.prefix = "classpath:/templates/"
		resolver.suffix = ".html"
		resolver.templateMode = TemplateMode.HTML
		resolver.characterEncoding = "UTF-8"
		resolver.isCacheable = false
		return resolver
	}

	@Bean
	fun textTemplateResolver(): SpringResourceTemplateResolver {
		val resolver = SpringResourceTemplateResolver()
		resolver.prefix = "classpath:/templates/"
		resolver.suffix = ".txt"
		resolver.templateMode = TemplateMode.TEXT
		resolver.characterEncoding = "UTF-8"
		resolver.isCacheable = false
		return resolver
	}

	@Bean
	fun templateEngine(
		htmlTemplateResolver: SpringResourceTemplateResolver,
		textTemplateResolver: SpringResourceTemplateResolver
	): SpringTemplateEngine {
		val templateEngine = SpringTemplateEngine()
		templateEngine.addTemplateResolver(htmlTemplateResolver)
		templateEngine.addTemplateResolver(textTemplateResolver)
		return templateEngine
	}
}